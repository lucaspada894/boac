package boa.test.datagen.java;

import java.io.IOException;

import org.junit.Test;

public class TestTypeLiteral extends Java8BaseTest {

	@Test
	public void typeLiteral() throws IOException {
		testWrapped(
			load("test/datagen/java/TypeLiteral.java").trim(),
			load("test/datagen/boa/TypeLiteral.boa").trim()
		);
	}
	
}
