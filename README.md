# Limited Creative
http://dev.bukkit.org/server-mods/limited-creative/

Required dependencies
---------------------

* [plib](https://github.com/possi/plib)
* [Bukkit](https://github.com/Bukkit/Bukkit)

Dependencies for optional integrations
--------------------------------------

* [WorldGuard](http://dev.bukkit.org/bukkit-plugins/worldguard/)
* [WorldEdit](http://dev.bukkit.org/bukkit-plugins/worldedit/)
* [xAuth](http://dev.bukkit.org/bukkit-plugins/xauth/)
* [AuthMe](http://dev.bukkit.org/bukkit-plugins/authme-reloaded/)
* [Multiverse-Core](http://dev.bukkit.org/bukkit-plugins/multiverse-core/)
* [Vault](http://dev.bukkit.org/bukkit-plugins/vault/)
* [Multiworld](http://dev.bukkit.org/bukkit-plugins/multiworld-v-2-0/)
* [LogBlock](http://dev.bukkit.org/bukkit-plugins/logblock/)

Building
--------
Download and install xAuth and Multiworld into the local `repo` repository with a command like so:

```bash
mvn install:install-file -Dfile=xAuth-2.0.26.jar -Dpackaging=jar -DlocalRepositoryPath=repo -DgroupId=de.luricos.bukkit -DartifactId=xAuth -Dversion=2.0.26
```