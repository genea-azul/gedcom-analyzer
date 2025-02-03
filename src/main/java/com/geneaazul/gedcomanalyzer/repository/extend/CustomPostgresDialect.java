package com.geneaazul.gedcomanalyzer.repository.extend;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.function.array.DdlTypeHelper;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.PatternFunctionDescriptorBuilder;
import org.hibernate.type.spi.TypeConfiguration;

public class CustomPostgresDialect extends PostgreSQLDialect {

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);

        SqmFunctionRegistry registry = functionContributions.getFunctionRegistry();
        TypeConfiguration types = functionContributions.getTypeConfiguration();

        new PatternFunctionDescriptorBuilder(registry, "ARRAY_AGG", FunctionKind.AGGREGATE, "ARRAY_AGG(?1)")
                .setExactArgumentCount(1)
                .setInvariantType(DdlTypeHelper.resolveArrayType(types.getBasicTypeForJavaType(String.class), types))
                .register();
    }

}
