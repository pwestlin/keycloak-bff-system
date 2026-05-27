// Roten behöver inte plugins-block här om du använder Version Catalogs i modulerna

subprojects {
    // 1. Vi måste vänta tills java-pluginet har blivit applicerat i modulen
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        }
    }
}