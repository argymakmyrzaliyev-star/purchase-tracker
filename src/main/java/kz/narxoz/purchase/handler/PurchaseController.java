package kz.narxoz.purchase.handler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import kz.narxoz.purchase.domain.PurchaseId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.beans.PropertyEditorSupport;
import java.net.URI;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(path = PurchaseController.PATH, produces = APPLICATION_JSON_VALUE)
public class PurchaseController {
    static final String PATH = "/api/v1/purchase-requests";

    private final PurchaseService purchases;

    public PurchaseController(PurchaseService purchases) {
        this.purchases = purchases;
    }

    @Operation(summary = "Create a purchase request")
    @ApiResponse(responseCode = "201", description = "Purchase request created",
            headers = @Header(name = "Location", description = "Path to the new purchase request",
                    schema = @Schema(type = "string")),
            content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = PurchaseResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Purchase business key already exists",
            content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class)))
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    ResponseEntity<PurchaseResponse> create(@Valid @RequestBody CreatePurchase body) {
        var purchase = purchases.register(PurchaseId.newId(), body.businessKey(), body.title());
        return ResponseEntity.created(URI.create(PATH + "/" + purchase.id().value()))
                .body(PurchaseResponse.from(purchase));
    }

    @InitBinder("id")
    void bindUuid(WebDataBinder binder) {
        binder.registerCustomEditor(UUID.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String value) {
                UUID id = UUID.fromString(value);
                if (!id.toString().equalsIgnoreCase(value)) {
                    throw new IllegalArgumentException("id must be a UUID");
                }
                setValue(id);
            }
        });
    }

    @Operation(summary = "Read a purchase request by UUID")
    @ApiResponse(responseCode = "200", description = "Purchase request found",
            content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = PurchaseResponse.class)))
    @ApiResponse(responseCode = "404", description = "Purchase request UUID does not exist",
            content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{id}")
    PurchaseResponse get(@PathVariable UUID id) {
        return PurchaseResponse.from(purchases.require(new PurchaseId(id)));
    }
}
