package com.enterprise.spendsync.core.internal.web;

import com.enterprise.spendsync.core.internal.dto.CreateFacilityRequest;
import com.enterprise.spendsync.core.internal.dto.FacilityResponse;
import com.enterprise.spendsync.core.internal.dto.UpdateFacilityRequest;
import com.enterprise.spendsync.core.internal.dto.UpdateStatusRequest;
import com.enterprise.spendsync.core.internal.service.FacilityService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.Organization.BASE + Endpoints.Organization.FACILITIES)
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping
    public ResponseEntity<List<FacilityResponse>> getAll(@RequestParam(required = false) UUID legalEntityId) {
        return ResponseEntity.ok(facilityService.getAllFacilities(legalEntityId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacilityResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(facilityService.getFacilityById(id));
    }

    @PostMapping
    public ResponseEntity<FacilityResponse> create(@Valid @RequestBody CreateFacilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facilityService.createFacility(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacilityResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateFacilityRequest request) {
        return ResponseEntity.ok(facilityService.updateFacility(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<FacilityResponse> updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(facilityService.updateStatus(id, request.isActive()));
    }
}
