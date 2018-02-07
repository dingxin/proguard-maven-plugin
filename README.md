# proguard-maven-plugin #

Maven Plugin for ProGuard


## Usage ##

Add plugin configuration into your project's pom.xml

```
<build>
	<plugins>
		<plugin>
			<groupId>com.github.dingxin</groupId>
			<artifactId>proguard-maven-plugin</artifactId>
			<version>1.0.0</version>
			<executions>
				<execution>
					<phase>package</phase>
					<goals>
						<goal>proguard</goal>
					</goals>
				</execution>
			</executions>
			<configuration>
				...
			</configuration>
		</plugin>
	</plugins>
</build>
```

Supported `<configuration>` children as below:

### Disables the plugin execution ###
```
<skip>false</skip>
```

### ProGuard options ###
```
<options>
	<option>-dontoptimize</option>
	<option>-keepattributes *Annotation*</option>
	...
</options>
```

### ProGuard configuration file ###
```
<configFile>proguard.conf</configFile>
```

### ProGuard Filters for the input jar ###
```
<inFilter>!module-info.class,!META-INF/maven/**</inFilter>
```

### ProGuard Filters for the output jar ###
```
<outFilter>!META-INF/maven/**</outFilter>
```

### Add dependency jars to -libraryjars arguments ###
```
<includeDependency>true</includeDependency>
```

### Add dependency jars to -injars arguments ###
```
<includeDependencyInjar>false</includeDependencyInjar>
```

### ProGuard Filters for dependency jars ###
```
<dependencyFilter>!module-info.class,!META-INF/**</dependencyFilter>
```

### Additional -libraryjars ###
```
<libs>
	<lib>${java.home}/jmods/java.base.jmod(!**.jar;!module-info.class)</lib>
</libs>
```


## Examples ##

### Example for Java SE 9 ###
```
<build>
	<plugins>
		<plugin>
			<groupId>com.github.dingxin</groupId>
			<artifactId>proguard-maven-plugin</artifactId>
			<version>1.0.0</version>
			<executions>
				<execution>
					<phase>package</phase>
					<goals>
						<goal>proguard</goal>
					</goals>
				</execution>
			</executions>
			<configuration>
				<libs>
					<lib>${java.home}/jmods/java.base.jmod(!**.jar;!module-info.class)</lib>
				</libs>
			</configuration>
		</plugin>
	</plugins>
</build>
```

### Example for Java SE 8 ###
```
<build>
	<plugins>
		<plugin>
			<groupId>com.github.dingxin</groupId>
			<artifactId>proguard-maven-plugin</artifactId>
			<version>1.0.0</version>
			<executions>
				<execution>
					<phase>package</phase>
					<goals>
						<goal>proguard</goal>
					</goals>
				</execution>
			</executions>
			<configuration>
				<libs>
					<lib>${java.home}/lib/rt.jar</lib>
					<lib>${java.home}/lib/jsse.jar</lib>
					<lib>${java.home}/lib/jce.jar</lib>
				</libs>
			</configuration>
		</plugin>
	</plugins>
</build>
```

### Example with Custom Options ###
```
<build>
	<plugins>
		<plugin>
			<groupId>com.github.dingxin</groupId>
			<artifactId>proguard-maven-plugin</artifactId>
			<version>1.0.0</version>
			<executions>
				<execution>
					<phase>package</phase>
					<goals>
						<goal>proguard</goal>
					</goals>
				</execution>
			</executions>
			<configuration>
				<options>
					<option>-dontoptimize</option>
					<option>-keepattributes *Annotation*</option>
					<option>-keepattributes Signature</option>
					<option>-keepattributes InnerClasses</option>
					<option>-keepclassmembers class * { @**.* *; }</option>
					<option>-keep public class * { public protected *; }</option>
					<option>-dontwarn org.apache.logging.log4j.**</option>
				</options>
				<libs>
					<lib>${java.home}/jmods/java.base.jmod(!**.jar;!module-info.class)</lib>
				</libs>
			</configuration>
		</plugin>
	</plugins>
</build>
```
