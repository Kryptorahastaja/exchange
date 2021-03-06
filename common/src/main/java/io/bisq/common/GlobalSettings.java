/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bisq.common;

import io.bisq.common.locale.TradeCurrency;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Locale;

public class GlobalSettings {
    private static boolean useAnimations = true;
    private static Locale locale = new Locale("en", "US");
    private static final ObjectProperty<Locale> localeProperty = new SimpleObjectProperty<>(locale);
    private static TradeCurrency defaultTradeCurrency;
    private static String btcDenomination;


    public static void setLocale(Locale locale) {
        GlobalSettings.locale = locale;
        localeProperty.set(locale);
    }

    public static void setUseAnimations(boolean useAnimations) {
        GlobalSettings.useAnimations = useAnimations;
    }

    public static void setDefaultTradeCurrency(TradeCurrency fiatCurrency) {
        GlobalSettings.defaultTradeCurrency = fiatCurrency;
    }


    public static void setBtcDenomination(String btcDenomination) {
        GlobalSettings.btcDenomination = btcDenomination;
    }

    public static TradeCurrency getDefaultTradeCurrency() {
        return defaultTradeCurrency;
    }

    public static String getBtcDenomination() {
        return btcDenomination;
    }

    public static ReadOnlyObjectProperty<Locale> localeProperty() {
        return localeProperty;
    }

    public static boolean getUseAnimations() {
        return useAnimations;
    }

    public static Locale getLocale() {
        return locale;
    }
}
