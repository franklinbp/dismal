# Project Index

## Documentacion
- `docs/architecture/PROJECT_OVERVIEW.md`: Resumen del backend y proposito general.
- `docs/requirements/APPS_DESIGN.md`: Flujos, wireframes y especificacion funcional.
- `docs/requirements/APPS_INTEGRATION.md`: Guia de integracion para Web/Android/Desktop.
- `docs/setup/MONOREPO_SETUP.md`: Pasos para crear el monorepo y proyectos.

## Archivos del backend tocados
- `src/main/java/com/dismal/dismal/config/JwtService.java`: JWT configurado por properties y expiracion correcta.
- `src/main/java/com/dismal/dismal/modules/sales/domain/Order.java`: relacion con License ahora Many-to-One para multi-activaciones.
- `src/main/java/com/dismal/dismal/modules/store/repository/LicenseRepository.java`: filtro por licencias activas + lock pesimista.
- `src/main/java/com/dismal/dismal/modules/sales/service/CheckoutService.java`: consumo de activaciones y validaciones.
- `src/main/java/com/dismal/dismal/modules/security/dto/CustomerDTO.java`: DTO seguro para clientes.
- `src/main/java/com/dismal/dismal/modules/security/service/CustomerService.java`: lista solo enabled y usa DTO.
- `src/main/java/com/dismal/dismal/modules/security/controller/CustomerController.java`: expone DTOs.
- `src/main/java/com/dismal/dismal/modules/security/repository/UserRepository.java`: queries con enabled.
- `src/main/java/com/dismal/dismal/modules/marketing/service/CampaignPublisherService.java`: filtra usuarios enabled.
