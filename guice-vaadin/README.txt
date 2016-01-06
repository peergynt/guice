Vaadin guice
======================

Vaadin guice is the official guice integration for Vaadin Framework version 7.

Getting started
----
See the tutorial at https://vaadin.com/wiki/-/wiki/Main/Vaadin+guice .

See also the companion add-on Vaadin guice Boot.

Changes in 1.0.0
----

* Fixed handling of URLs not ending in a slash.
* Support just slash as UI mapping
* Made VaadinServlet overridable by defining the bean "vaadinServlet" in the application configuration

Changes in 1.0.0.beta3
----

* Fixed Vaadin guice Boot interoperation with guice MVC.
* Fixed context path handling.  

Changes in 1.0.0.beta2
----

Vaadin guice Boot 1.0.0.beta2 uses guice @ConfigurationProperties to make configuration of the Vaadin servlet more flexible.

The access denied view is presented when configured and the user tries to navigate to a non-existent view.

Changes in 1.0.0.beta1
----

There are several API changes in Vaadin guice 1.0.0.beta1 compared to earlier alpha versions.
These aim for a stable API, and include:

API renames:
* The parameter name for @guiceUI is "path"
* The parameter name for @guiceView is "name"
* VaadinUIScope -> UIScope
* VaadinViewScope -> ViewScope
* Some annotations and classes have moved to new packages
* com.vaadin.guice.servlet.guiceAwareUIProvider -> com.vaadin.guice.server.GuiceUIProvider
* com.vaadin.guice.servlet.guiceAwareVaadinServlet -> com.vaadin.guice.server.GuiceVaadinServlet

Other changes:
* The default mapping for @guiceUI is the context root and there is no convention based auto-generation of the path
* It is possible to omit the leading slash in @guiceUI path mapping
* Added @VaadinSessionScope
* ViewProviderAccessDelegate has been split to ViewAccessControl and ViewInstanceAccessControl
* Using @guiceView on a class not implementing View stops view scanning

Migrating from vaadin4guice
----
Vaadin guice contains a subset of the functionality of the add-on vaadin4guice.
The community add-on vaadin4guice will be updated to extend Vaadin guice to provide additional functionality not present in Vaadin guice.

As the naming of packages and some annotations have changed, the following renames and import updates are typically required for migration:
* org.vaadin.guice -> com.vaadin.guice (for the annotations and classes included in Vaadin guice)
* @VaadinUI -> @guiceUI
* @VaadinView -> @guiceView

Note also that the ui parameter of @VaadinView now also covers the subclasses of the listed UI classes.

Issue tracking
----
Issues for the add-on are tracked in the Vaadin Trac at http://dev.vaadin.com

Building Vaadin guice
----
See the parent project page at https://github.com/vaadin/guice .

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
