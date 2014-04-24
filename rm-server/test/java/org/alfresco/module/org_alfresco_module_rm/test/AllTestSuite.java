/*
 * Copyright (C) 2005-2011 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.module.org_alfresco_module_rm.test;

import org.alfresco.module.org_alfresco_module_rm.test.integration.IntegrationTestSuite;
import org.alfresco.repo.ParameterProcessorTestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


/**
 * Convenience test suite that runs all the tests.
 *
 * @author Roy Wetherall
 * @since 2.1
 */
@RunWith(Suite.class)
@SuiteClasses(
{
    ActionTestSuite.class,
    CapabilitiesTestSuite.class,
    ServicesTestSuite.class,
    WebScriptTestSuite.class,
    ParameterProcessorTestSuite.class,
    IntegrationTestSuite.class
})
public class AllTestSuite
{
}
