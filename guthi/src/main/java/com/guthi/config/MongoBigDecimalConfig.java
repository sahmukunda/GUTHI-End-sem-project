package com.guthi.config;

import java.math.BigDecimal;
import java.util.List;
import org.bson.types.Decimal128;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

/**
 * Spring Data MongoDB stores {@link BigDecimal} as a string by default, which breaks every {@code
 * $sum} aggregation (strings cannot be summed, so totals come out as 0). Register explicit
 * converters so BigDecimal fields are persisted as BSON {@link Decimal128} instead.
 */
@Configuration
public class MongoBigDecimalConfig {

    @Bean
    MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(
                List.of(new BigDecimalToDecimal128Converter(), new Decimal128ToBigDecimalConverter()));
    }

    @WritingConverter
    static class BigDecimalToDecimal128Converter implements Converter<BigDecimal, Decimal128> {
        @Override
        public Decimal128 convert(BigDecimal source) {
            return new Decimal128(source);
        }
    }

    @ReadingConverter
    static class Decimal128ToBigDecimalConverter implements Converter<Decimal128, BigDecimal> {
        @Override
        public BigDecimal convert(Decimal128 source) {
            return source.bigDecimalValue();
        }
    }
}
