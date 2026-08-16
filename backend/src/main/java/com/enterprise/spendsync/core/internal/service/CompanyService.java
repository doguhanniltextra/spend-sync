package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.dto.CompanyResponse;
import com.enterprise.spendsync.core.internal.dto.CreateCompanyRequest;

public interface CompanyService {
    CompanyResponse createCompany(CreateCompanyRequest request);
}
