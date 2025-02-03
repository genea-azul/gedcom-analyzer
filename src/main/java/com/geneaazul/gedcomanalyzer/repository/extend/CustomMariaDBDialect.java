package com.geneaazul.gedcomanalyzer.repository.extend;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.PatternFunctionDescriptorBuilder;
import org.hibernate.type.spi.TypeConfiguration;

public class CustomMariaDBDialect extends MariaDBDialect {

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);

        SqmFunctionRegistry registry = functionContributions.getFunctionRegistry();
        TypeConfiguration types = functionContributions.getTypeConfiguration();

        new PatternFunctionDescriptorBuilder(registry, "ARRAY_AGG", FunctionKind.AGGREGATE, "JSON_ARRAYAGG(DISTINCT ?1)")
                .setExactArgumentCount(1)
                .setInvariantType(types.getBasicTypeForJavaType(String.class))
                .register();
    }

}
