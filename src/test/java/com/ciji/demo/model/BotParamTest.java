package com.ciji.demo.model;

import com.openpojo.reflection.impl.PojoClassFactory;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.rule.impl.GetterMustExistRule;
import com.openpojo.validation.rule.impl.SetterMustExistRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class BotParamTest {

    @Test
    public void testContract() {
        ValidatorBuilder.create()
                .with(new GetterMustExistRule())
                .with(new SetterMustExistRule())
                .with(new GetterTester())
                .with(new SetterTester())
                .build().validate(PojoClassFactory.getPojoClass(BotParam.class));
        EqualsVerifier.forClass(BotParam.class)
                .verify();
    }
}
