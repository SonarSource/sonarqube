load("github.com/SonarSource/cirrus-modules@v2", "load_features")
load("cirrus", "env", "fs", "yaml")


def main(ctx):
    if env.get("CIRRUS_REPO_FULL_NAME") == 'SonarSource/sonar-enterprise':
        features = yaml.dumps(load_features(ctx, only_if=dict()))
        doc = fs.read("private/.cirrus.yml")
        return features + doc

    # On SonarSource/sonarqube repo, we don't trigger any Cirrus build
    return []
