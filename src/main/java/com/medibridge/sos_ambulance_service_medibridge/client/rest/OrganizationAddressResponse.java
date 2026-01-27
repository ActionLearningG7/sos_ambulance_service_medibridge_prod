package com.medibridge.sos_ambulance_service_medibridge.client.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationAddressResponse {
    private String name;
    private String street;
    private String city;
    private String zipCode;
    private String country;
    private Double latitude;
    private Double longitude;
}
