package ja.burhanrashid52.photoeditor;

import android.graphics.Bitmap;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;


public class SaveSettingsTest {

    @Test
    public void testByDefaultTransparentAndClearViewFlagSettingIsEnabled() {
        SaveSettings saveSettings = new SaveSettings();
        assertTrue(saveSettings.isClearViewsEnabled());
        assertTrue(saveSettings.isTransparencyEnabled());
    }

    @Test
    public void testWhenTransparentSettingIsDisabled() {
        SaveSettings saveSettings = new SaveSettings(false, null, null, null);


        assertFalse(saveSettings.isTransparencyEnabled());
        assertTrue(saveSettings.isClearViewsEnabled());
    }

    @Test
    public void testWhenClearViewAfterSaveSettingIsDisabled() {
        SaveSettings saveSettings = new SaveSettings(null, false, null, null);


        assertFalse(saveSettings.isClearViewsEnabled());
        assertTrue(saveSettings.isTransparencyEnabled());
    }

    @Test
    public void testWhenBothTransparentClearViewAfterSaveSettingIsDisabled() {
        SaveSettings saveSettings = new SaveSettings(false, false, null, null);


        assertFalse(saveSettings.isClearViewsEnabled());
        assertFalse(saveSettings.isTransparencyEnabled());
    }

    @Test
    public void testDefaultCompressAndQualitySaveSettings() {
        SaveSettings saveSettings = new SaveSettings(null, null, null, null);

        assertEquals(saveSettings.getCompressFormat(), Bitmap.CompressFormat.PNG);
        assertEquals((int) saveSettings.getCompressQuality(), 100);
    }

    @Test
    public void testCompressAndQualitySaveSettingsValues() {

        Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;
        int compressQuality = 50;

        SaveSettings saveSettings = new SaveSettings(null, null, compressFormat, compressQuality);


        assertEquals(saveSettings.getCompressFormat(), compressFormat);
        assertEquals((int) saveSettings.getCompressQuality(), compressQuality);
    }
}