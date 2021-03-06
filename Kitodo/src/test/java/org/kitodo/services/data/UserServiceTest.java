/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.services.data;

import org.junit.Test;
import org.kitodo.services.ServiceManager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserServiceTest {

    private static final UserService userService = new ServiceManager().getUserService();

    @Test
    public void shouldCheckIfIsLoginValid() {
        assertTrue("Login is invalid!",
            userService.isLoginValid("validLogin", "src/test/resources/test_loginBlackList.txt"));

        assertFalse("Login is valid!", userService.isLoginValid("root", "src/test/resources/test_loginBlackList.txt"));
    }
}
