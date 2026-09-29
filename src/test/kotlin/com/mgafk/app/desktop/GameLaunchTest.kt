package com.mgafk.app.desktop
import org.junit.Assert.*
import org.junit.Test
class GameLaunchTest {
    @Test fun bareDefaultHostWorks() { assertEquals("https://magicgarden.gg/r/My%20Room", GameLaunch.url("magicgarden.gg", "My Room")) }
    @Test fun prefixedHostWorks() { assertEquals("https://magicgarden.gg/r/Room", GameLaunch.url("https://magicgarden.gg/", "Room")) }
    @Test fun roomCannotInjectQueryOrPath() { assertEquals("https://magicgarden.gg/r/a%2Fb%3Fx%3D1", GameLaunch.url("", "a/b?x=1")) }
    @Test fun rawTokenBecomesGameCookie() { assertEquals("mc_jwt=abc.def.ghi", GameLaunch.cookie("abc.def.ghi")) }
    @Test fun cookieIsNotPrefixedTwice() { assertEquals("mc_jwt=abc", GameLaunch.cookie("mc_jwt=abc")) }
    @Test(expected = IllegalArgumentException::class) fun rejectPlainHttp() { GameLaunch.url("http://magicgarden.gg", "Room") }
    @Test(expected = IllegalArgumentException::class) fun rejectEmptyRoom() { GameLaunch.url("", " ") }
    @Test(expected = IllegalArgumentException::class) fun rejectCookieInjection() { GameLaunch.cookie("abc; other=bad") }
}
