// File generated from our OpenAPI spec
package com.stripe.service;

import com.google.gson.reflect.TypeToken;
import com.stripe.exception.StripeException;
import com.stripe.model.Margin;
import com.stripe.model.StripeCollection;
import com.stripe.net.ApiMode;
import com.stripe.net.ApiRequestParams;
import com.stripe.net.ApiResource;
import com.stripe.net.ApiService;
import com.stripe.net.BaseAddress;
import com.stripe.net.RequestOptions;
import com.stripe.net.StripeResponseGetter;
import com.stripe.param.MarginCreateParams;
import com.stripe.param.MarginListParams;
import com.stripe.param.MarginRetrieveParams;
import com.stripe.param.MarginUpdateParams;

public final class MarginService extends ApiService {
  public MarginService(StripeResponseGetter responseGetter) {
    super(responseGetter);
  }

  /**
   * Create a margin object to be used with invoices, invoice items, and invoice line items for a
   * customer to represent a partner discount.A margin has a {@code percent_off} which is the
   * percent that will be taken off the subtotal after all items and other discounts and promotions)
   * of any invoices for a customer. Calculation of prorations do not include any partner margins
   * applied on the original invoice item.
   */
  public Margin create(MarginCreateParams params) throws StripeException {
    return create(params, (RequestOptions) null);
  }
  /**
   * Create a margin object to be used with invoices, invoice items, and invoice line items for a
   * customer to represent a partner discount.A margin has a {@code percent_off} which is the
   * percent that will be taken off the subtotal after all items and other discounts and promotions)
   * of any invoices for a customer. Calculation of prorations do not include any partner margins
   * applied on the original invoice item.
   */
  public Margin create(MarginCreateParams params, RequestOptions options) throws StripeException {
    String path = "/v1/billing/margins";
    return getResponseGetter()
        .request(
            BaseAddress.API,
            ApiResource.RequestMethod.POST,
            path,
            ApiRequestParams.paramsToMap(params),
            Margin.class,
            options,
            ApiMode.V1);
  }
  /** Retrieve a list of your margins. */
  public StripeCollection<Margin> list(MarginListParams params) throws StripeException {
    return list(params, (RequestOptions) null);
  }
  /** Retrieve a list of your margins. */
  public StripeCollection<Margin> list(RequestOptions options) throws StripeException {
    return list((MarginListParams) null, options);
  }
  /** Retrieve a list of your margins. */
  public StripeCollection<Margin> list() throws StripeException {
    return list((MarginListParams) null, (RequestOptions) null);
  }
  /** Retrieve a list of your margins. */
  public StripeCollection<Margin> list(MarginListParams params, RequestOptions options)
      throws StripeException {
    String path = "/v1/billing/margins";
    return getResponseGetter()
        .request(
            BaseAddress.API,
            ApiResource.RequestMethod.GET,
            path,
            ApiRequestParams.paramsToMap(params),
            new TypeToken<StripeCollection<Margin>>() {}.getType(),
            options,
            ApiMode.V1);
  }
  /** Retrieve a margin object with the given ID. */
  public Margin retrieve(String margin, MarginRetrieveParams params) throws StripeException {
    return retrieve(margin, params, (RequestOptions) null);
  }
  /** Retrieve a margin object with the given ID. */
  public Margin retrieve(String margin, RequestOptions options) throws StripeException {
    return retrieve(margin, (MarginRetrieveParams) null, options);
  }
  /** Retrieve a margin object with the given ID. */
  public Margin retrieve(String margin) throws StripeException {
    return retrieve(margin, (MarginRetrieveParams) null, (RequestOptions) null);
  }
  /** Retrieve a margin object with the given ID. */
  public Margin retrieve(String margin, MarginRetrieveParams params, RequestOptions options)
      throws StripeException {
    String path = String.format("/v1/billing/margins/%s", ApiResource.urlEncodeId(margin));
    return getResponseGetter()
        .request(
            BaseAddress.API,
            ApiResource.RequestMethod.GET,
            path,
            ApiRequestParams.paramsToMap(params),
            Margin.class,
            options,
            ApiMode.V1);
  }
  /** Update the specified margin object. Certain fields of the margin object are not editable. */
  public Margin update(String margin, MarginUpdateParams params) throws StripeException {
    return update(margin, params, (RequestOptions) null);
  }
  /** Update the specified margin object. Certain fields of the margin object are not editable. */
  public Margin update(String margin, RequestOptions options) throws StripeException {
    return update(margin, (MarginUpdateParams) null, options);
  }
  /** Update the specified margin object. Certain fields of the margin object are not editable. */
  public Margin update(String margin) throws StripeException {
    return update(margin, (MarginUpdateParams) null, (RequestOptions) null);
  }
  /** Update the specified margin object. Certain fields of the margin object are not editable. */
  public Margin update(String margin, MarginUpdateParams params, RequestOptions options)
      throws StripeException {
    String path = String.format("/v1/billing/margins/%s", ApiResource.urlEncodeId(margin));
    return getResponseGetter()
        .request(
            BaseAddress.API,
            ApiResource.RequestMethod.POST,
            path,
            ApiRequestParams.paramsToMap(params),
            Margin.class,
            options,
            ApiMode.V1);
  }
}
