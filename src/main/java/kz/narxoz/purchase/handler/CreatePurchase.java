package kz.narxoz.purchase.handler;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePurchase(
        @NotBlank String businessKey,
        @NotBlank @Size(max = 120) String title
) {}
