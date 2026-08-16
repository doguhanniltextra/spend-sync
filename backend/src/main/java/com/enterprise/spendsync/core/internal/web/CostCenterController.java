package com.enterprise.spendsync.core.internal.web;

import com.enterprise.spendsync.core.internal.dto.CostCenterResponse;
import com.enterprise.spendsync.core.internal.dto.CreateCostCenterRequest;
import com.enterprise.spendsync.core.internal.dto.UpdateCostCenterRequest;
import com.enterprise.spendsync.core.internal.dto.UpdateStatusRequest;
import com.enterprise.spendsync.core.internal.service.CostCenterService;
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
@RequestMapping(Endpoints.Organization.BASE + Endpoints.Organization.COST_CENTERS)
public class CostCenterController {

    private final CostCenterService costCenterService;

    public CostCenterController(CostCenterService costCenterService) {
        this.costCenterService = costCenterService;
    }

    @GetMapping
    public ResponseEntity<List<CostCenterResponse>> getAll(@RequestParam(required = false) UUID legalEntityId) {
        return ResponseEntity.ok(costCenterService.getAllCostCenters(legalEntityId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CostCenterResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(costCenterService.getCostCenterById(id));
    }

    @PostMapping
    public ResponseEntity<CostCenterResponse> create(@Valid @RequestBody CreateCostCenterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(costCenterService.createCostCenter(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CostCenterResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateCostCenterRequest request) {
        return ResponseEntity.ok(costCenterService.updateCostCenter(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CostCenterResponse> updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(costCenterService.updateStatus(id, request.isActive()));
    }
}
