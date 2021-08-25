package com.appland.appmap.config;

/**
 * Data structure for holding configuration of a package
 */
public class PackageConfig {

    public final String packageName;
    public final String includedPackageName;
    public final boolean shallow;

    /**
     * Return configuration of a package
     * @param packageName name of the package
     * @param includedPackageName name of the nearest included parent package found in appmap.yml
     * @param shallow shallow config value of the package, taken from the nearest parent package found in appmap.yml
     */
    public PackageConfig(String packageName, String includedPackageName, boolean shallow) {
        this.packageName = packageName;
        this.includedPackageName = includedPackageName;
        this.shallow = shallow;
    }
}
