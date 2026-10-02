package com.dismal.distribuciones.modules.sales.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderPaidEvent extends ApplicationEvent {

    private final String customerEmail;
    private final String customerPhone;
    private final String softwareName;
    private final String licenseSerial;

    /**
     * Create a new OrderPaidEvent.
     * @param source the object on which the event initially occurred or with which the event is associated (never {@code null})
     * @param customerEmail Email of the customer
     * @param customerPhone Phone number of the customer
     * @param softwareName Name of the purchased software
     * @param licenseSerial The purchased license key (in plaintext)
     */
    public OrderPaidEvent(Object source, String customerEmail, String customerPhone, String softwareName, String licenseSerial) {
        super(source);
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.softwareName = softwareName;
        this.licenseSerial = licenseSerial;
    }
}
