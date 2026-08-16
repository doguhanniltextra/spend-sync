package com.enterprise.spendsync.core.internal.web;

import com.enterprise.spendsync.core.internal.dto.CreateLegalEntityRequest;
import com.enterprise.spendsync.core.internal.dto.LegalEntityResponse;
import com.enterprise.spendsync.core.internal.dto.UpdateLegalEntityRequest;
import com.enterprise.spendsync.core.internal.dto.UpdateStatusRequest;
import com.enterprise.spendsync.core.internal.service.LegalEntityService;
import com.enterprise.spendsync.shared.config.Endpoints;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.Organization.BASE + Endpoints.Organization.LEGAL_ENTITIES)
public class LegalEntityController {

    private final LegalEntityService legalEntityService;

    public LegalEntityController(LegalEntityService legalEntityService) {
        this.legalEntityService = legalEntityService;
    }

    @GetMapping
    public ResponseEntity<List<LegalEntityResponse>> getAll() {
        return ResponseEntity.ok(legalEntityService.getAllLegalEntities());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LegalEntityResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(legalEntityService.getLegalEntityById(id));
    }

    @PostMapping
    public ResponseEntity<LegalEntityResponse> create(@Valid @RequestBody CreateLegalEntityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(legalEntityService.createLegalEntity(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LegalEntityResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateLegalEntityRequest request) {
        return ResponseEntity.ok(legalEntityService.updateLegalEntity(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<LegalEntityResponse> updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(legalEntityService.updateStatus(id, request.isActive()));
    }
}
