This folder contains expected blame data for corresponding repositories (stored in test-repos folder). The data in expected-blame folder is
produced by blaming each file with
git blame {file_name_from_the_repository} --date=iso-strict --show-email -l --root >  expected-blame/repository-name/{file_name_from_the_repository}

The expected blame data is then used in the corresponding unit and integration tests to assert correctness of the new git blame algorithm
implemented in the scanner engine.