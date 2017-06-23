class MyClass:
  while True:
    return False #Noncompliant

  def __enter__(self):
    pass
  def __exit__(self, exc_type, exc_val):  # Noncompliant
    pass
