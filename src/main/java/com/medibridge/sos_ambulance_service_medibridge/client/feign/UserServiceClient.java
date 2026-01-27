package com.medibridge.sos_ambulance_service_medibridge.client.feign;

import com.medibridge.sos_ambulance_service_medibridge.client.rest.OrganizationAddressResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "user-service-medibridge", url = "${user-service.url:http://localhost:8081}")
public interface UserServiceClient {

    @GetMapping("/api/v1/organization/address")
    OrganizationAddressResponse getOrganizationAddress();
}
