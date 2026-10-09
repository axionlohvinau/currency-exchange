import com.aksion.currencyexchange.dao.CurrencyDao;
import com.aksion.currencyexchange.dao.ExchangeRateDao;
import com.aksion.currencyexchange.model.Currency;
import com.aksion.currencyexchange.model.ExchangeRate;
import com.aksion.currencyexchange.model.ExchangeResult;
import com.aksion.currencyexchange.service.ExchangeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExchangeServiceTest {

    private static final Currency USD = new Currency(
            1,
            "USD",
            "United States dollar",
            "$"
    );

    private static final Currency EUR = new Currency(
            2,
            "EUR",
            "Euro",
            "€"
    );

    private static final Currency BYN = new Currency(
            3,
            "BYN",
            "Belarusian ruble",
            "Br"
    );

    private TestExchangeRateDao rateDao;
    private ExchangeService service;

    @BeforeEach
    void setUp() {
        rateDao = new TestExchangeRateDao();

        service = new ExchangeService(
                new TestCurrencyDao(),
                rateDao
        );
    }

    @Test
    void convertsUsingDirectRate() throws SQLException {
        rateDao.add(USD, EUR, "0.95");

        ExchangeResult result = service.exchange(
                "USD",
                "EUR",
                new BigDecimal("100")
        ).orElseThrow();

        assertEquals(
                new BigDecimal("95.00"),
                result.convertedAmount()
        );

        assertEquals("USD", result.baseCurrency().code());
        assertEquals("EUR", result.targetCurrency().code());
    }

    @Test
    void convertsUsingReverseRate() throws SQLException {
        rateDao.add(USD, EUR, "0.95");

        ExchangeResult result = service.exchange(
                "EUR",
                "USD",
                new BigDecimal("100")
        ).orElseThrow();

        assertEquals(
                new BigDecimal("105.26"),
                result.convertedAmount()
        );
    }

    @Test
    void convertsUsingCrossRate() throws SQLException {
        rateDao.add(USD, EUR, "0.95");
        rateDao.add(USD, BYN, "3.20");

        ExchangeResult result = service.exchange(
                "EUR",
                "BYN",
                new BigDecimal("100")
        ).orElseThrow();

        assertEquals(
                new BigDecimal("336.84"),
                result.convertedAmount()
        );
    }

    @Test
    void returnsEmptyWhenRateIsMissing() throws SQLException {
        Optional<ExchangeResult> result = service.exchange(
                "USD",
                "EUR",
                new BigDecimal("100")
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyWhenCurrencyIsMissing() throws SQLException {
        Optional<ExchangeResult> result = service.exchange(
                "USD",
                "ZZZ",
                new BigDecimal("100")
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void roundsHalfUpToTwoDecimalPlaces() throws SQLException {
        rateDao.add(USD, EUR, "0.125");

        ExchangeResult result = service.exchange(
                "USD",
                "EUR",
                BigDecimal.ONE
        ).orElseThrow();

        assertEquals(
                new BigDecimal("0.13"),
                result.convertedAmount()
        );
    }

    private static class TestCurrencyDao extends CurrencyDao {

        private final Map<String, Currency> currencies = Map.of(
                "USD", USD,
                "EUR", EUR,
                "BYN", BYN
        );

        @Override
        public Optional<Currency> findByCode(String code) {
            return Optional.ofNullable(currencies.get(code));
        }
    }

    private static class TestExchangeRateDao extends ExchangeRateDao {

        private final Map<String, ExchangeRate> rates = new HashMap<>();

        void add(Currency base, Currency target, String rate) {
            rates.put(
                    base.code() + "-" + target.code(),
                    new ExchangeRate(
                            rates.size() + 1,
                            base,
                            target,
                            new BigDecimal(rate)
                    )
            );
        }

        @Override
        public Optional<ExchangeRate> findByCodes(
                String baseCode,
                String targetCode
        ) {
            return Optional.ofNullable(
                    rates.get(baseCode + "-" + targetCode)
            );
        }
    }
}