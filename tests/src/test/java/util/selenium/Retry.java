/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package util.selenium;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriverException;

import static java.util.concurrent.TimeUnit.SECONDS;

class Retry {
  public static final Retry _30_SECONDS = new Retry(30, SECONDS);

  private final long timeoutInMs;

  Retry(long duration, TimeUnit timeUnit) {
    this.timeoutInMs = timeUnit.toMillis(duration);
  }

  <T> void execute(Supplier<Optional<T>> target, Consumer<T> action) {
    WebDriverException lastError = null;

    boolean retried = false;

    long start = System.currentTimeMillis();
    while ((System.currentTimeMillis() - start) < timeoutInMs) {
      try {
        Optional<T> targetElement = target.get();
        if (targetElement.isPresent()) {
          action.accept(targetElement.get());
          if (retried) {
            System.out.println();
          }
          return;
        }
      } catch (StaleElementReferenceException e) {
        // ignore
      } catch (WebDriverException e) {
        lastError = e;
      }

      retried = true;
      System.out.print(".");
    }

    if (retried) {
      System.out.println();
    }

    if (lastError != null) {
      throw lastError;
    }
    throw new NoSuchElementException("Not found");
  }

  <T> void execute(Runnable action) {
    WebDriverException lastError = null;

    boolean retried = false;

    long start = System.currentTimeMillis();
    while ((System.currentTimeMillis() - start) < timeoutInMs) {
      try {
        action.run();
        if (retried) {
          System.out.println();
        }
        return;
      } catch (StaleElementReferenceException e) {
        // ignore
      } catch (WebDriverException e) {
        lastError = e;
      }

      retried = true;
      System.out.print(".");
    }

    if (retried) {
      System.out.println();
    }

    if (lastError != null) {
      throw lastError;
    }
    throw new NoSuchElementException("Not found");
  }

  <T> boolean verify(Supplier<T> targetSupplier, Predicate<T> predicate) throws NoSuchElementException {
    Error error = Error.KO;

    boolean retried = false;

    long start = System.currentTimeMillis();
    while ((System.currentTimeMillis() - start) < timeoutInMs) {
      try {
        if (predicate.apply(targetSupplier.get())) {
          if (retried) {
            System.out.println();
          }
          return true;
        }

        error = Error.KO;
      } catch (InvalidElementStateException e) {
        error = Error.KO;
      } catch (NotFoundException e) {
        error = Error.NOT_FOUND;
      } catch (StaleElementReferenceException e) {
        // ignore
      }

      retried = true;
      System.out.print(".");
    }

    if (retried) {
      System.out.println();
    }

    if (error == Error.NOT_FOUND) {
      throw new NoSuchElementException("Not found");
    }
    return false;
  }

  enum Error {
    NOT_FOUND, KO
  }
}
