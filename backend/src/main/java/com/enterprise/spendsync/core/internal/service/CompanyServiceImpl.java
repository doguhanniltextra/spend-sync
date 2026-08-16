package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.dto.CompanyResponse;
import com.enterprise.spendsync.core.internal.dto.CreateCompanyRequest;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.shared.exception.CompanyAlreadyExistsException;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.exception.UserAlreadyHasCompanyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final LegalEntityRepository legalEntityRepository;

    public CompanyServiceImpl(UserRepository userRepository,
                              TenantRepository tenantRepository,
                              LegalEntityRepository legalEntityRepository) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.legalEntityRepository = legalEntityRepository;
    }

    @Override
    public CompanyResponse createCompany(CreateCompanyRequest request) {
        // 1. Verify User existence
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new SpendSyncException("User with id '" + request.userId() + "' was not found.", HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {});

        // 2. Check if user already owns/belongs to a company
        if (user.getTenant() != null) {
            throw new UserAlreadyHasCompanyException("User '" + user.getEmail() + "' is already associated with company '" + user.getTenant().getName() + "'.");
        }

        String companyName = request.companyName().trim();
        String slug = toSlug(companyName);

        // 3. Verify uniqueness of Tenant Name and Slug
        if (tenantRepository.existsByName(companyName) || tenantRepository.existsBySlug(slug)) {
            throw new CompanyAlreadyExistsException(companyName);
        }

        // 4. Create and persist Tenant
        Tenant tenant = new Tenant(companyName, slug);
        Tenant savedTenant = tenantRepository.save(tenant);

        // 5. Create and persist primary Legal Entity
        LegalEntity legalEntity = new LegalEntity(
                savedTenant,
                request.legalEntityName().trim(),
                request.companyCode().trim().toUpperCase(),
                request.taxNumber().trim(),
                request.baseCurrency().trim().toUpperCase(),
                request.registeredAddress().trim(),
                request.country().trim().toUpperCase()
        );
        if (request.taxOffice() != null && !request.taxOffice().isBlank()) {
            legalEntity.setTaxOffice(request.taxOffice().trim());
        }
        LegalEntity savedLegalEntity = legalEntityRepository.save(legalEntity);

        // 6. Link User to Tenant and default Legal Entity as ROOT_USER
        user.setTenant(savedTenant);
        user.assignLegalEntity(savedLegalEntity);
        user.addRole(RoleType.ROOT_USER);
        userRepository.save(user);

        return CompanyResponse.fromEntities(savedTenant, savedLegalEntity, user.getId());
    }

    /**
     * Converts raw company name to a URL-friendly, lowercase, hyphenated slug.
     */
    private String toSlug(String input) {
        if (input == null) {
            return "";
        }
        String normalized = input.trim().toLowerCase(Locale.ENGLISH)
                .replace("ı", "i")
                .replace("ğ", "g")
                .replace("ü", "u")
                .replace("ş", "s")
                .replace("ö", "o")
                .replace("ç", "c");

        String noWhitespace = WHITESPACE.matcher(normalized).replaceAll("-");
        String normalizedString = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalizedString).replaceAll("");
        return slug.replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }
}
