load("github.com/SonarSource/cirrus-modules@v2", "load_features")
load("cirrus", "env", "fs", "yaml")


def main(ctx):
    if env.get("CIRRUS_REPO_FULL_NAME") == 'SonarSource/sonar-enterprise':
        features = yaml.dumps(load_features(ctx, only_if=dict()))
        doc = fs.read("private/.cirrus.yml")
    else:
        features = yaml.dumps(load_features(ctx, features=["build_number"]))
        doc = fs.read(".cirrus-public.yml")
    return features + doc
