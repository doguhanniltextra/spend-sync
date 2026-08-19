package com.enterprise.spendsync.catalog.internal.service;

import com.enterprise.spendsync.catalog.dto.CatalogCategoryCreateRequest;
import com.enterprise.spendsync.catalog.dto.CatalogCategoryDto;
import com.enterprise.spendsync.catalog.dto.CatalogItemCreateRequest;
import com.enterprise.spendsync.catalog.dto.CatalogItemResponse;
import com.enterprise.spendsync.catalog.dto.CatalogItemUpdateRequest;
import com.enterprise.spendsync.catalog.dto.CsvImportResultDto;
import com.enterprise.spendsync.catalog.dto.CsvRowErrorDto;
import com.enterprise.spendsync.catalog.internal.domain.CatalogCategory;
import com.enterprise.spendsync.catalog.internal.domain.CatalogItem;
import com.enterprise.spendsync.catalog.internal.repository.CatalogCategoryRepository;
import com.enterprise.spendsync.catalog.internal.repository.CatalogItemRepository;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.purchasing.internal.repository.VendorRepository;
import com.enterprise.spendsync.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CatalogAdminServiceImpl implements CatalogAdminService {

    private final CatalogItemRepository itemRepository;
    private final CatalogCategoryRepository categoryRepository;
    private final VendorRepository vendorRepository;
    private final TenantRepository tenantRepository;

    public CatalogAdminServiceImpl(CatalogItemRepository itemRepository,
                                   CatalogCategoryRepository categoryRepository,
                                   VendorRepository vendorRepository,
                                   TenantRepository tenantRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.vendorRepository = vendorRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public CatalogItemResponse createCatalogItem(UUID tenantId, CatalogItemCreateRequest request, User currentUser) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));

        String itemCode = request.itemCode();
        if (itemCode != null && !itemCode.trim().isBlank()) {
            itemCode = itemCode.trim().toUpperCase();
            if (itemRepository.findByTenantIdAndItemCode(tenantId, itemCode).isPresent()) {
                throw new IllegalArgumentException("Item with code '" + itemCode + "' already exists in this tenant.");
            }
        } else {
            itemCode = generateItemCode(request.name());
        }

        CatalogCategory category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findByTenantIdAndId(tenantId, request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()));
        }

        Vendor vendor = null;
        if (request.preferredVendorId() != null) {
            vendor = vendorRepository.findByIdAndTenantId(request.preferredVendorId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + request.preferredVendorId()));
        }

        CatalogItem item = new CatalogItem(
                tenant,
                itemCode,
                request.name(),
                request.description(),
                category,
                vendor,
                request.unitPrice(),
                request.currency(),
                request.vatRate(),
                request.unitOfMeasure(),
                request.contractReference(),
                request.validFrom(),
                request.validUntil(),
                request.isPreferred() != null && request.isPreferred(),
                request.glAccountCode(),
                currentUser
        );

        CatalogItem saved = itemRepository.save(item);
        return mapToResponse(saved);
    }

    @Override
    public CatalogItemResponse updateCatalogItem(UUID tenantId, UUID itemId, CatalogItemUpdateRequest request) {
        CatalogItem item = itemRepository.findByTenantIdAndId(tenantId, itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CatalogItem not found: " + itemId));

        item.setName(request.name());
        item.setDescription(request.description());
        item.setUnitPrice(request.unitPrice());
        if (request.currency() != null && !request.currency().isBlank()) {
            item.setCurrency(request.currency());
        }
        if (request.vatRate() != null) {
            item.setVatRate(request.vatRate());
        }
        if (request.unitOfMeasure() != null && !request.unitOfMeasure().isBlank()) {
            item.setUnitOfMeasure(request.unitOfMeasure());
        }
        item.setContractReference(request.contractReference());
        item.setValidFrom(request.validFrom());
        item.setValidUntil(request.validUntil());
        if (request.isPreferred() != null) {
            item.setPreferred(request.isPreferred());
        }
        if (request.isActive() != null) {
            item.setActive(request.isActive());
        }
        item.setGlAccountCode(request.glAccountCode());

        if (request.categoryId() != null) {
            CatalogCategory category = categoryRepository.findByTenantIdAndId(tenantId, request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()));
            item.setCategory(category);
        } else {
            item.setCategory(null);
        }

        if (request.preferredVendorId() != null) {
            Vendor vendor = vendorRepository.findByIdAndTenantId(request.preferredVendorId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + request.preferredVendorId()));
            item.setPreferredVendor(vendor);
        } else {
            item.setPreferredVendor(null);
        }

        CatalogItem saved = itemRepository.save(item);
        return mapToResponse(saved);
    }

    @Override
    public void deleteCatalogItem(UUID tenantId, UUID itemId) {
        CatalogItem item = itemRepository.findByTenantIdAndId(tenantId, itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CatalogItem not found: " + itemId));
        // Soft delete
        item.setActive(false);
        itemRepository.save(item);
    }

    @Override
    public CatalogCategoryDto createCategory(UUID tenantId, CatalogCategoryCreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));

        CatalogCategory parent = null;
        if (request.parentId() != null) {
            parent = categoryRepository.findByTenantIdAndId(tenantId, request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found: " + request.parentId()));
        }

        String code = request.code();
        if (code == null || code.isBlank()) {
            code = "CAT-" + request.name().trim().toUpperCase().replaceAll("[^A-Z0-9]+", "-");
        }

        CatalogCategory category = new CatalogCategory(
                tenant,
                parent,
                code,
                request.name(),
                request.iconCode(),
                request.description()
        );

        CatalogCategory saved = categoryRepository.save(category);
        return new CatalogCategoryDto(
                saved.getId(),
                saved.getCode(),
                saved.getName(),
                saved.getFullPath(),
                saved.getIconCode(),
                saved.getDescription(),
                saved.getParent() != null ? saved.getParent().getId() : null,
                0,
                Collections.emptyList()
        );
    }

    @Override
    public CsvImportResultDto importCatalogFromCsv(UUID tenantId, InputStream csvInputStream, User currentUser) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));

        List<CsvRowErrorDto> errors = new ArrayList<>();
        int totalRows = 0;
        int successCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvInputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new CsvImportResultDto(0, 0, 0, List.of(new CsvRowErrorDto(0, "FILE", "CSV file is empty")));
            }

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isBlank()) {
                    continue;
                }
                totalRows++;

                try {
                    List<String> columns = parseCsvLine(line);
                    if (columns.size() < 6) {
                        errors.add(new CsvRowErrorDto(rowNumber, line, "Insufficient column count (at least 6 columns required)"));
                        continue;
                    }

                    String itemCode = columns.get(0).trim().toUpperCase();
                    String name = columns.get(1).trim();
                    String description = columns.size() > 2 ? columns.get(2).trim() : "";
                    String categoryPath = columns.size() > 3 ? columns.get(3).trim() : "";
                    String vendorIdentifier = columns.size() > 4 ? columns.get(4).trim() : "";
                    String priceStr = columns.get(5).trim();

                    if (name.isBlank()) {
                        errors.add(new CsvRowErrorDto(rowNumber, itemCode, "Item name cannot be blank"));
                        continue;
                    }

                    BigDecimal unitPrice;
                    try {
                        unitPrice = new BigDecimal(priceStr.replace(",", "."));
                    } catch (Exception e) {
                        errors.add(new CsvRowErrorDto(rowNumber, itemCode, "Invalid unit price: " + priceStr));
                        continue;
                    }

                    BigDecimal vatRate = new BigDecimal("0.20");
                    if (columns.size() > 6 && !columns.get(6).trim().isBlank()) {
                        try {
                            vatRate = new BigDecimal(columns.get(6).trim().replace(",", "."));
                        } catch (Exception ignored) {}
                    }

                    String uom = columns.size() > 7 && !columns.get(7).trim().isBlank() ? columns.get(7).trim() : "PIECE";
                    String contractRef = columns.size() > 8 ? columns.get(8).trim() : null;
                    LocalDate validFrom = null;
                    if (columns.size() > 9 && !columns.get(9).trim().isBlank()) {
                        try {
                            validFrom = LocalDate.parse(columns.get(9).trim(), DateTimeFormatter.ISO_DATE);
                        } catch (Exception ignored) {}
                    }
                    LocalDate validUntil = null;
                    if (columns.size() > 10 && !columns.get(10).trim().isBlank()) {
                        try {
                            validUntil = LocalDate.parse(columns.get(10).trim(), DateTimeFormatter.ISO_DATE);
                        } catch (Exception ignored) {}
                    }
                    boolean isPreferred = false;
                    if (columns.size() > 11 && !columns.get(11).trim().isBlank()) {
                        isPreferred = Boolean.parseBoolean(columns.get(11).trim());
                    }

                    // Resolve or create category hierarchy
                    CatalogCategory category = resolveOrCreateCategory(tenant, categoryPath);

                    // Resolve vendor if specified
                    Vendor vendor = resolveVendor(tenantId, vendorIdentifier);

                    if (itemCode.isBlank()) {
                        itemCode = generateItemCode(name);
                    }

                    Optional<CatalogItem> existingOpt = itemRepository.findByTenantIdAndItemCode(tenantId, itemCode);
                    CatalogItem item;
                    if (existingOpt.isPresent()) {
                        item = existingOpt.get();
                        item.setName(name);
                        item.setDescription(description);
                        item.setCategory(category);
                        item.setPreferredVendor(vendor);
                        item.setUnitPrice(unitPrice);
                        item.setVatRate(vatRate);
                        item.setUnitOfMeasure(uom);
                        item.setContractReference(contractRef);
                        item.setValidFrom(validFrom);
                        item.setValidUntil(validUntil);
                        item.setPreferred(isPreferred);
                        item.setActive(true);
                    } else {
                        item = new CatalogItem(
                                tenant,
                                itemCode,
                                name,
                                description,
                                category,
                                vendor,
                                unitPrice,
                                "TRY",
                                vatRate,
                                uom,
                                contractRef,
                                validFrom,
                                validUntil,
                                isPreferred,
                                null,
                                currentUser
                        );
                    }

                    itemRepository.save(item);
                    successCount++;
                } catch (Exception ex) {
                    errors.add(new CsvRowErrorDto(rowNumber, "ROW-" + rowNumber, "Processing error: " + ex.getMessage()));
                }
            }
        } catch (Exception e) {
            errors.add(new CsvRowErrorDto(0, "PARSER", "Failed to read CSV: " + e.getMessage()));
        }

        return new CsvImportResultDto(totalRows, successCount, errors.size(), errors);
    }

    @Override
    public byte[] exportCatalogToCsv(UUID tenantId) {
        List<CatalogItem> items = itemRepository.findByTenantId(tenantId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            writer.println("item_code,name,description,category_path,vendor_name,unit_price,vat_rate,uom,contract_ref,valid_from,valid_until,is_preferred,is_active");
            for (CatalogItem item : items) {
                String catPath = item.getCategory() != null ? item.getCategory().getFullPath() : "";
                String vendorName = item.getPreferredVendor() != null ? item.getPreferredVendor().getName() : "";
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,%s,\"%s\",\"%s\",\"%s\",\"%s\",%b,%b%n",
                        escapeCsv(item.getItemCode()),
                        escapeCsv(item.getName()),
                        escapeCsv(item.getDescription() != null ? item.getDescription() : ""),
                        escapeCsv(catPath),
                        escapeCsv(vendorName),
                        item.getUnitPrice(),
                        item.getVatRate(),
                        escapeCsv(item.getUnitOfMeasure()),
                        escapeCsv(item.getContractReference() != null ? item.getContractReference() : ""),
                        item.getValidFrom() != null ? item.getValidFrom().toString() : "",
                        item.getValidUntil() != null ? item.getValidUntil().toString() : "",
                        item.isPreferred(),
                        item.isActive()
                );
            }
        }
        return out.toByteArray();
    }

    private CatalogCategory resolveOrCreateCategory(Tenant tenant, String categoryPath) {
        if (categoryPath == null || categoryPath.isBlank()) {
            return null;
        }

        String[] parts = categoryPath.split("[/>]");
        CatalogCategory currentParent = null;
        StringBuilder currentPath = new StringBuilder();

        for (String rawPart : parts) {
            String partName = rawPart.trim();
            if (partName.isBlank()) continue;

            if (currentPath.length() > 0) {
                currentPath.append(" / ");
            }
            currentPath.append(partName);

            String fullPath = currentPath.toString();
            Optional<CatalogCategory> existing = categoryRepository.findByTenantIdAndFullPath(tenant.getId(), fullPath);
            if (existing.isPresent()) {
                currentParent = existing.get();
            } else {
                String code = "CAT-" + partName.toUpperCase().replaceAll("[^A-Z0-9]+", "-");
                CatalogCategory newCategory = new CatalogCategory(tenant, currentParent, code, partName, null, null);
                newCategory.setFullPath(fullPath);
                currentParent = categoryRepository.save(newCategory);
            }
        }

        return currentParent;
    }

    private Vendor resolveVendor(UUID tenantId, String vendorIdentifier) {
        if (vendorIdentifier == null || vendorIdentifier.isBlank()) {
            return null;
        }
        List<Vendor> vendors = vendorRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
        return vendors.stream()
                .filter(v -> v.getName().equalsIgnoreCase(vendorIdentifier.trim())
                        || v.getTaxNumber().equalsIgnoreCase(vendorIdentifier.trim()))
                .findFirst()
                .orElse(null);
    }

    private String generateItemCode(String name) {
        String base = name != null && !name.isBlank()
                ? name.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "-")
                : "ITEM";
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        return base + "-" + System.currentTimeMillis() % 100000;
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString().trim());
        return result;
    }

    private String escapeCsv(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"");
    }

    private CatalogItemResponse mapToResponse(CatalogItem item) {
        String contractAlert = computeContractAlert(item.getValidUntil());

        return new CatalogItemResponse(
                item.getId(),
                item.getItemCode(),
                item.getName(),
                item.getDescription(),
                item.getCategory() != null ? item.getCategory().getId() : null,
                item.getCategory() != null ? item.getCategory().getName() : null,
                item.getCategory() != null ? item.getCategory().getFullPath() : null,
                item.getPreferredVendor() != null ? item.getPreferredVendor().getId() : null,
                item.getPreferredVendor() != null ? item.getPreferredVendor().getName() : null,
                item.getPreferredVendor() != null ? item.getPreferredVendor().getTaxNumber() : null,
                item.getPreferredVendor() != null && item.getPreferredVendor().getTier() != null ? item.getPreferredVendor().getTier().name() : null,
                item.getUnitPrice(),
                item.getCurrency(),
                item.getVatRate(),
                item.getUnitOfMeasure(),
                item.getContractReference(),
                item.getValidFrom(),
                item.getValidUntil(),
                item.isActive(),
                item.isPreferred(),
                item.getGlAccountCode(),
                contractAlert,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private String computeContractAlert(LocalDate validUntil) {
        if (validUntil == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        if (validUntil.isBefore(today)) {
            return "⚠️ CONTRACT EXPIRED (Expired on: " + validUntil + ")";
        }
        long daysRemaining = ChronoUnit.DAYS.between(today, validUntil);
        if (daysRemaining <= 7) {
            return "⚠️ CRITICAL CONTRACT EXPIRY: Contract expires in " + daysRemaining + " days (" + validUntil + ").";
        }
        if (daysRemaining <= 30) {
            return "ℹ️ Contract expiry notice: Contract expires in " + daysRemaining + " days (" + validUntil + ").";
        }
        return null;
    }
}
