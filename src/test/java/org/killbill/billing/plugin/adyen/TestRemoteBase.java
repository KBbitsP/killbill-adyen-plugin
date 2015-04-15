/*
 * Copyright 2014 Groupon, Inc
 *
 * Groupon licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.adyen;

import java.io.IOException;
import java.util.Properties;

import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.AdyenPaymentPortRegistry;
import org.killbill.billing.plugin.adyen.client.PaymentPortRegistry;
import org.killbill.billing.plugin.adyen.client.jaxws.HttpHeaderInterceptor;
import org.killbill.billing.plugin.adyen.client.jaxws.LoggingInInterceptor;
import org.killbill.billing.plugin.adyen.client.jaxws.LoggingOutInterceptor;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverterManagement;
import org.killbill.billing.plugin.adyen.client.payment.converter.impl.PaymentInfoConverterService;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentRequestSender;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderHostedPaymentPagePort;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderPort;
import org.killbill.billing.plugin.adyen.client.payment.service.Signer;
import org.testng.annotations.BeforeClass;

public abstract class TestRemoteBase {

    // To run these tests, you need a properties file in the classpath (e.g. src/test/resources/adyen.properties)
    // See README.md for details on the required properties
    private static final String PROPERTIES_FILE_NAME = "adyen.properties";

    // Simulate payments using a credit card in Germany (requires credentials for a German merchant account)
    protected static final Currency DEFAULT_CURRENCY = Currency.EUR;
    protected static final String DEFAULT_COUNTRY = "DE";

    // Magic details at https://www.adyen.com/home/support/knowledgebase/implementation-articles.html
    // Note: make sure to use the Amex one, as Visa/MC is not always configured by default
    protected static final String CC_NUMBER = "370000000000002";
    protected static final int CC_EXPIRATION_MONTH = 8;
    protected static final int CC_EXPIRATION_YEAR = 2018;
    protected static final String CC_VERIFICATION_VALUE = "7373";

    protected AdyenConfigProperties adyenConfigProperties;
    protected AdyenPaymentServiceProviderPort adyenPaymentServiceProviderPort;
    protected AdyenPaymentServiceProviderHostedPaymentPagePort adyenPaymentServiceProviderHostedPaymentPagePort;

    @BeforeClass(groups = "slow")
    public void setUpBeforeClass() throws Exception {
        adyenConfigProperties = getAdyenConfigProperties();

        final PaymentInfoConverterManagement paymentInfoConverterManagement = new PaymentInfoConverterService();

        final Signer signer = new Signer(adyenConfigProperties);
        final AdyenRequestFactory adyenRequestFactory = new AdyenRequestFactory(paymentInfoConverterManagement, adyenConfigProperties, signer);

        final LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
        final LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
        final HttpHeaderInterceptor httpHeaderInterceptor = new HttpHeaderInterceptor();
        final PaymentPortRegistry adyenPaymentPortRegistry = new AdyenPaymentPortRegistry(adyenConfigProperties, loggingInInterceptor, loggingOutInterceptor, httpHeaderInterceptor);
        final AdyenPaymentRequestSender adyenPaymentRequestSender = new AdyenPaymentRequestSender(adyenPaymentPortRegistry);

        adyenPaymentServiceProviderPort = new AdyenPaymentServiceProviderPort(paymentInfoConverterManagement, adyenRequestFactory, adyenPaymentRequestSender);
        adyenPaymentServiceProviderHostedPaymentPagePort = new AdyenPaymentServiceProviderHostedPaymentPagePort(adyenConfigProperties, adyenRequestFactory);
    }

    private AdyenConfigProperties getAdyenConfigProperties() throws IOException {
        final Properties properties = TestUtils.loadProperties(PROPERTIES_FILE_NAME);
        return new AdyenConfigProperties(properties);
    }
}
