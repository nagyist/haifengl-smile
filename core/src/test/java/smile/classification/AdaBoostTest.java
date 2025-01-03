/*
 * Copyright (c) 2010-2021 Haifeng Li. All rights reserved.
 *
 * Smile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Smile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Smile.  If not, see <https://www.gnu.org/licenses/>.
 */

package smile.classification;

import smile.data.type.StructField;
import smile.datasets.BreastCancer;
import smile.datasets.Iris;
import smile.io.Read;
import smile.io.Write;
import smile.math.MathEx;
import smile.test.data.*;
import smile.validation.*;
import smile.validation.metric.Accuracy;
import smile.validation.metric.Error;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Haifeng
 */
public class AdaBoostTest {
    
    public AdaBoostTest() {
    }

    @BeforeAll
    public static void setUpClass() throws Exception {
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testWeather() throws Exception {
        System.out.println("Weather");

        MathEx.setSeed(19650218); // to get repeatable results.
        AdaBoost model = AdaBoost.fit(WeatherNominal.formula, WeatherNominal.data, 20, 5, 8, 1);
        String[] fields = model.schema().names();

        double[] importance = model.importance();
        for (int i = 0; i < importance.length; i++) {
            System.out.format("%-15s %.4f%n", fields[i], importance[i]);
        }

        double[] shap = model.shap(WeatherNominal.data);
        System.out.println("----- SHAP -----");
        for (int i = 0; i < fields.length; i++) {
            System.out.format("%-15s %.4f    %.4f%n", fields[i], shap[2*i], shap[2*i+1]);
        }

        java.nio.file.Path temp = Write.object(model);
        Read.object(temp);

        ClassificationMetrics metrics = LOOCV.classification(WeatherNominal.formula, WeatherNominal.data,
                (f, x) -> AdaBoost.fit(f, x, 20, 5, 8, 1));
        System.out.println(metrics);
        assertEquals(0.6429, metrics.accuracy(), 1E-4);
    }

    @Test
    public void testIris() throws Exception {
        System.out.println("Iris");

        MathEx.setSeed(19650218); // to get repeatable results.
        var iris = new Iris();
        AdaBoost model = AdaBoost.fit(iris.formula(), iris.data(), 200, 20, 4, 5);

        double[] importance = model.importance();
        for (int i = 0; i < importance.length; i++) {
            System.out.format("%-15s %.4f%n", model.schema().names()[i], importance[i]);
        }

        ClassificationMetrics metrics = LOOCV.classification(iris.formula(), iris.data(),
                (f, x) -> AdaBoost.fit(f, x, 200, 20, 4, 1));
        System.out.println(metrics);
        assertEquals(0.9533, metrics.accuracy(), 1E-4);
    }

    @Test
    public void testPenDigits() {
        System.out.println("Pen Digits");

        MathEx.setSeed(19650218); // to get repeatable results.
        ClassificationValidations<AdaBoost> result = CrossValidation.classification(10, PenDigits.formula, PenDigits.data,
                (f, x) -> AdaBoost.fit(f, x, 200, 20, 4, 1));
        System.out.println(result);
        assertEquals(0.9525, result.avg.accuracy(), 1E-4);
    }

    @Test
    public void testBreastCancer() throws Exception {
        System.out.println("Breast Cancer");

        MathEx.setSeed(19650218); // to get repeatable results.
        var cancer = new BreastCancer();
        var result = CrossValidation.classification(10, cancer.formula(), cancer.data(),
                (f, x) -> AdaBoost.fit(f, x, 100, 20, 4, 1));

        System.out.println(result);
        int error = result.rounds.stream().mapToInt(round -> round.metrics.error()).sum();
        assertEquals(15, error);
    }

    @Test
    public void testSegment() {
        System.out.println("Segment");

        MathEx.setSeed(19650218); // to get repeatable results.
        AdaBoost model = AdaBoost.fit(Segment.formula, Segment.train, 200, 20, 6, 1);

        double[] importance = model.importance();
        for (int i = 0; i < importance.length; i++) {
            System.out.format("%-15s %.4f%n", model.schema().names()[i], importance[i]);
        }

        int error = Error.of(Segment.testy, model.predict(Segment.test));
        System.out.println("Error = " + error);
        assertEquals(30, error);

        System.out.println("----- Progressive Accuracy -----");
        int[][] test = model.test(Segment.test);
        for (int i = 0; i < test.length; i++) {
            System.out.format("Accuracy with %3d trees: %.4f%n", i + 1, Accuracy.of(Segment.testy, test[i]));
        }
    }

    @Test
    public void testUSPS() {
        System.out.println("USPS");

        MathEx.setSeed(19650218); // to get repeatable results.
        AdaBoost model = AdaBoost.fit(USPS.formula, USPS.train, 200, 20, 64, 1);

        double[] importance = model.importance();
        for (int i = 0; i < importance.length; i++) {
            System.out.format("%-15s %.4f%n", model.schema().names()[i], importance[i]);
        }

        int error = Error.of(USPS.testy, model.predict(USPS.test));
        System.out.println("Error = " + error);
        assertEquals(152, error);

        System.out.println("----- Progressive Accuracy -----");
        int[][] test = model.test(USPS.test);
        for (int i = 0; i < test.length; i++) {
            System.out.format("Accuracy with %3d trees: %.4f%n", i+1, Accuracy.of(USPS.testy, test[i]));
        }
    }

    @Test
    public void testShap() throws Exception {
        System.out.println("SHAP");

        MathEx.setSeed(19650218); // to get repeatable results.
        var iris = new Iris();
        AdaBoost model = AdaBoost.fit(iris.formula(), iris.data(), 200, 20, 4, 5);
        String[] fields = java.util.Arrays.stream(model.schema().fields()).map(StructField::name).toArray(String[]::new);
        double[] importance = model.importance();
        double[] shap = model.shap(iris.data());

        System.out.println("----- importance -----");
        for (int i = 0; i < importance.length; i++) {
            System.out.format("%-15s %.4f%n", fields[i], importance[i]);
        }

        System.out.println("----- SHAP -----");
        for (int i = 0; i < fields.length; i++) {
            System.out.format("%-15s %.4f    %.4f    %.4f%n", fields[i], shap[2*i], shap[2*i+1], shap[2*i+2]);
        }
    }
}
