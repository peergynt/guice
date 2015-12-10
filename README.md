Vaadin guice
======================

Vaadin guice is the official guice integration for Vaadin Framework version 7.

Building the project
----
Execute `mvn clean install` in the root directory to build vaadin-guice and vaadin-guice-boot.

Porting from vaadin-spring
----

first, you need to install it:
```bash
cd YOUR_SOURCE_FOLDER
git clone https://github.com/berndhopp/guice.git
cd guice
mvn install
```

then, open bugrap in your IDE, replace

```
'spring' -> 'guice'
'Spring' -> 'Guice'
'import org.springframework.beans.factory.annotation.Autowired;' -> import com.google.inject.Inject;''
'@Autowired' -> '@Inject'
'@SpringComponent' -> ''
'import org.springframework.stereotype.Component;' -> ''
```

If you have any classes that you want to be singleton, you need to annotate them with @com.google.inject.Singleton, since
in contrast to spring, guice will default to a 'prototype' strategy ( that is why @SpringComponent can be removed without replacement ).

Your SpringVaadinServlet needs to be adjusted manually, GuiceVaadinServlet expects a @Configuration annotation like this:

```Java
@Configuration(modules = {MyModule.class}, basePackage = "com.mycompany")
@WebServlet(urlPatterns = "/*", name = "MyServlet", asyncSupported = true)
public static class MyServlet extends GuiceVaadinServlet {
}
```

Issue tracking
----
Issues for the project are tracked in the Vaadin Trac at http://dev.vaadin.com

Contributions
----
Contributions to the project can be done using the Vaadin Gerrit review system at http://dev.vaadin.com/review. Pull requests cannot be accepted due to Gerrit being used for code review.


Copyright 2015 Vaadin Ltd.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
