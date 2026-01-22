package org.sonar.server.dummy;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DummyClassTest {
    
    private DummyClass dummyClass = new DummyClass();
    
    @Test
    public void add_shouldReturnSum() {
        assertThat(dummyClass.add(2, 3)).isEqualTo(5);
        assertThat(dummyClass.add(0, 0)).isEqualTo(0);
        assertThat(dummyClass.add(-1, 1)).isEqualTo(0);
    }
    
    @Test
    public void isPositive_shouldReturnTrueForPositiveNumbers() {
        assertThat(dummyClass.isPositive(1)).isTrue();
        assertThat(dummyClass.isPositive(100)).isTrue();
    }
    
    @Test
    public void isPositive_shouldReturnFalseForNonPositiveNumbers() {
        assertThat(dummyClass.isPositive(0)).isFalse();
        assertThat(dummyClass.isPositive(-1)).isFalse();
    }
    
    @Test
    public void getMessage_shouldReturnExpectedMessage() {
        assertThat(dummyClass.getMessage()).isEqualTo("Hello from DummyClass");
    }
}
