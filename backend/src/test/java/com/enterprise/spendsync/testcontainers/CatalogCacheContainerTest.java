package com.enterprise.spendsync.testcontainers;

import com.enterprise.spendsync.catalog.dto.CatalogCategoryDto;
import com.enterprise.spendsync.catalog.internal.domain.CatalogCategory;
import com.enterprise.spendsync.catalog.internal.repository.CatalogCategoryRepository;
import com.enterprise.spendsync.catalog.internal.service.CatalogService;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CatalogCacheContainerTest extends AbstractContainerIntegrationTest {

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private CatalogCategoryRepository categoryRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("TC-CT-07 & TC-CT-08: Should query database on first call and automatically populate real Redis with 6h TTL")
    void shouldCacheCatalogCategoryTreeInRealRedis() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Tenant tenant = tenantRepository.save(new Tenant("Cache Tenant " + uid, "cache-tenant-" + uid));
        UUID tenantId = tenant.getId();

        CatalogCategory itCat = new CatalogCategory(tenant, null, "IT-EQUIP", "IT Equipment", "laptop", "Hardware & IT");
        categoryRepository.save(itCat);

        String redisExpectedKey = "catalog:categories::" + tenantId;

        // Ensure key does not exist yet in Redis
        stringRedisTemplate.delete(redisExpectedKey);

        // 2. First Service Call (Cache Miss -> DB Query -> Auto write to Redis)
        List<CatalogCategoryDto> firstResult = catalogService.getCategoryTree(tenantId);
        assertThat(firstResult).hasSize(1);
        assertThat(firstResult.get(0).name()).isEqualTo("IT Equipment");

        // 3. Verify Redis key exists in real Redis 7.2 container
        Boolean keyExists = stringRedisTemplate.hasKey(redisExpectedKey);
        assertThat(keyExists).isTrue();

        // 4. Verify TTL is around 6 hours (between 21500 and 21600 seconds)
        Long ttl = stringRedisTemplate.getExpire(redisExpectedKey);
        assertThat(ttl).isNotNull();
        assertThat(ttl).isGreaterThan(21000L).isLessThanOrEqualTo(21600L);

        // 5. Verify cached JSON content
        String cachedJson = stringRedisTemplate.opsForValue().get(redisExpectedKey);
        assertThat(cachedJson).isNotBlank();
        assertThat(cachedJson).contains("IT Equipment");
        assertThat(cachedJson).contains("IT-EQUIP");

        // 6. Second Service Call (Cache Hit -> Returns directly from Redis)
        List<CatalogCategoryDto> secondResult = catalogService.getCategoryTree(tenantId);
        assertThat(secondResult).hasSize(1);
        assertThat(secondResult.get(0).code()).isEqualTo("IT-EQUIP");
    }
}
