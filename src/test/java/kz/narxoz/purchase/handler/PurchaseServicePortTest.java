package kz.narxoz.purchase.handler;

import kz.narxoz.purchase.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Fast unit tests: the outbound port is mocked; no database or HTTP calls. */
@ExtendWith(MockitoExtension.class)
class PurchaseServicePortTest {
    @Mock
    private PurchaseRepository purchases;

    private PurchaseService service;

    @BeforeEach
    void setUp() {
        Rule rules = (from, to) -> {
            new UnapprovedCannotOrder().check(from, to);
            new TransitionRule().check(from, to);
        };
        service = new PurchaseService(rules, purchases);
    }

    @Test
    void registerSendsDraftToMockedOutboundPort() {
        PurchaseId id = PurchaseId.newId();
        PurchaseRequest result = service.register(id, "PR-19", "Office laptop");

        ArgumentCaptor<PurchaseRequest> saved = ArgumentCaptor.forClass(PurchaseRequest.class);
        verify(purchases).insert(saved.capture());
        assertEquals(new PurchaseRequest(id, new PurchaseKey("PR-19"),
                PurchaseStatus.DRAFT, "Office laptop"), saved.getValue());
        assertEquals(result, saved.getValue());
        verifyNoMoreInteractions(purchases);
    }

    @Test
    void countReadsTheOutboundPort() {
        when(purchases.count(new PurchaseKey("PR-19"))).thenReturn(1L);
        assertEquals(1, service.count("PR-19"));
        verify(purchases).count(new PurchaseKey("PR-19"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void invalidBusinessKeyNeverReachesDatabase(String key) {
        assertThrows(IllegalArgumentException.class,
                () -> service.register(PurchaseId.newId(), key, "Office laptop"));
        verifyNoInteractions(purchases);
    }
}
