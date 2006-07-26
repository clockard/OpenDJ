/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2006 Sun Microsystems, Inc.
 */
The Integration Test Suite is designed to provide a level of testing
above that provided by standard unit tests. It is principally designed
for developers to check for regressions after modifying the OpenDS code.
The Integration Test Suite is built upon the testng open source framework.

The Integration Test Suite will run in the developer's workspace in either
one of two modes. First, the "quick and efficient" mode allows 
the Integration Test to be run with very minimal setup. In this mode,
the Integration Test Suite will install OpenDS, configure OpenDS,
create the test scripts, create the testng xml configuration file, 
and run the Integration Test Suite in one command.

Second, a more advanced mode allows more flexible configurations for 
OpenDS and the Integration Test Suite. It also requires that users
install and configure OpenDS and configure the testng xml file themselves.

Linux and Solaris unix environments.
To run the Integration Test Suite, use the following steps.

1.  Make sure that you have the Integration Test Suite code.
It is located in trunk/opends/tests/integration-tests-testng
directory.

2.  cd to trunk/opends/tests/integration-tests-testng.

3.  execute ./build.sh
This command builds the Integration Test Suite classes and creates the test.sh script.
The OpenDS package will also be built if it does not exist.
The options for running the test.sh script and the absolute directory path for
the test.sh script are written to standard out.

4.0 To run the Integration Test Suite, use either 4.1 or 4.2. The "quick and efficient"
mode that is described in 4.1 is recommended. 

4.1 For the "quick and efficient" mode, 
execute [absolute path]/test.sh installOpenDS [installation directory] [port number]
where the "installation directory" is the directory where you wish to install OpenDS,
and the "port number" is the port that will be used for communication with OpenDS.
The "port number" is used for non-SSL connections. Typically 389 is used.

Be sure that there is not a previous installation of OpenDS in the installation
directory. Also be sure that the port number is available. 

4.2 For the advanced mode, you will need to install and configure OpenDS at the
location of your choice. A fresh installation is necessary. You will also need
to create or edit the testng.xml file that is found in 
trunk/opends/tests/integration-tests-testng/ext/testng.
If you previously ran step 4.1, testng.xml file will exist and you can edit it.

Also, you will need the test.sh file. The easiest way to create one is to run 
step 4.1 once.

For the advanced mode, 
execute [absolute path]/test.sh [OpenDS top-level directory] 

5.0 After following either steps 4.1 or 4.2, output for each test should
appear in standard out telling the outcome of the test (PASS, FAIL, or SKIP).

The output from OpenDS for each test should appear in files in
trunk/opends/tests/integration-tests-testng/opends/logs directory.


