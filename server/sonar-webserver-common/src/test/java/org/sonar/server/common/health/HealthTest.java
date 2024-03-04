package org.sonar.server.common.health;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.server.health.Health;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class HealthTest {

  @Mock
  private Health.Builder mockBuilder;

  @Test
  public void testHealthBuilder() {
    // Initialize mocks
    MockitoAnnotations.initMocks(this);

    // Mock the behavior of the builder
    when(mockBuilder.setStatus(Health.Status.GREEN)).thenReturn(mockBuilder);
    when(mockBuilder.addCause("Test Cause")).thenReturn(mockBuilder);
    when(mockBuilder.build()).thenReturn(Health.GREEN);

    // Use the mock builder
    Health health = mockBuilder.setStatus(Health.Status.GREEN)
        .addCause("Test Cause")
        .build();

    // Verify the interactions
    verify(mockBuilder, times(1)).setStatus(Health.Status.GREEN);
    verify(mockBuilder, times(1)).addCause("Test Cause");
    verify(mockBuilder, times(1)).build();

    // Assert the result
    assertThat(health).isEqualTo(Health.GREEN);
  }

  @Test
  public void testHealthEquals() {
    Health health1 = new Health.Builder().setStatus(Health.Status.GREEN).build();
    Health health2 = new Health.Builder().setStatus(Health.Status.GREEN).build();
    Health health3 = new Health.Builder().setStatus(Health.Status.RED).build();

    assertThat(health1).isEqualTo(health2);
    assertThat(health1).isNotEqualTo(health3);
  }
}
