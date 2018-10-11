package com.axelor.apps.gst.service;

import com.axelor.apps.account.db.Account;
import com.axelor.apps.account.db.AccountManagement;
import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.Tax;
import com.axelor.apps.account.db.TaxEquiv;
import com.axelor.apps.account.db.TaxLine;
import com.axelor.apps.account.service.AccountManagementAccountService;
import com.axelor.apps.account.service.AnalyticMoveLineService;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.service.CurrencyService;
import com.axelor.apps.base.service.PriceListService;
import com.axelor.apps.base.service.tax.AccountManagementService;
import com.axelor.apps.base.service.tax.FiscalPositionService;
import com.axelor.apps.purchase.service.PurchaseProductService;
import com.axelor.apps.supplychain.service.InvoiceLineSupplychainService;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class InvoiceLineGstServiceImpl extends InvoiceLineSupplychainService
    implements InvoiceLineGstService {

  @Inject
  public InvoiceLineGstServiceImpl(
      AccountManagementService accountManagementService,
      CurrencyService currencyService,
      PriceListService priceListService,
      AppAccountService appAccountService,
      AnalyticMoveLineService analyticMoveLineService,
      AccountManagementAccountService accountManagementAccountService,
      PurchaseProductService purchaseProductService) {
    super(
        accountManagementService,
        currencyService,
        priceListService,
        appAccountService,
        analyticMoveLineService,
        accountManagementAccountService,
        purchaseProductService);
    // TODO Auto-generated constructor stub
  }

  @Override
  public Map<String, Object> fillProductInformation(Invoice invoice, InvoiceLine invoiceLine)
      throws AxelorException {

    Map<String, Object> productInformation = new HashMap<>();

    Product product = invoiceLine.getProduct();
    boolean isPurchase = this.isPurchase(invoice);
    TaxLine taxLine;

    try {
      taxLine = getTaxLine(invoice, invoiceLine, isPurchase);

      productInformation.put("taxLine", taxLine);
      productInformation.put("taxRate", taxLine.getValue());
      productInformation.put("taxCode", taxLine.getTax().getCode());

      Tax tax =
          accountManagementAccountService.getProductTax(
              accountManagementAccountService.getAccountManagement(product, invoice.getCompany()),
              isPurchase);
      TaxEquiv taxEquiv =
          Beans.get(FiscalPositionService.class)
              .getTaxEquiv(invoice.getPartner().getFiscalPosition(), tax);
      productInformation.put("taxEquiv", taxEquiv);

      // getting correct account for the product
      AccountManagement accountManagement =
          accountManagementAccountService.getAccountManagement(product, invoice.getCompany());
      Account account =
          accountManagementAccountService.getProductAccount(accountManagement, isPurchase);
      productInformation.put("account", account);
    } catch (AxelorException e) {
      taxLine = null;
      productInformation.put("taxLine", null);
      productInformation.put("taxRate", null);
      productInformation.put("taxCode", null);
      productInformation.put("taxEquiv", null);
      productInformation.put("account", null);
    }
    BigDecimal price = this.getUnitPrice(invoice, invoiceLine, taxLine, isPurchase);

    productInformation.put("productName", invoiceLine.getProduct().getName());
    productInformation.put("unit", this.getUnit(invoiceLine.getProduct(), isPurchase));

    // getting correct account for the product
    AccountManagement accountManagement =
        accountManagementAccountService.getAccountManagement(product, invoice.getCompany());
    Account account =
        accountManagementAccountService.getProductAccount(accountManagement, isPurchase);
    productInformation.put("account", account);

    Map<String, Object> discounts = this.getDiscount(invoice, invoiceLine, price);

    if (discounts != null) {
      productInformation.put("discountAmount", discounts.get("discountAmount"));
      productInformation.put("discountTypeSelect", discounts.get("discountTypeSelect"));
      if (discounts.get("price") != null) {
        price = (BigDecimal) discounts.get("price");
      }
    }
    productInformation.put("gstRate", product.getGstRate());
    productInformation.put("hsbn", product.getHsbn());
    productInformation.put("price", price);
    return productInformation;
  }

  @Override
  public InvoiceLine calculateInvoiceLine(Invoice invoice, InvoiceLine invoiceLine) {
    if (invoice.getCompany().getAddress() == null || invoice.getAddress() == null) {
      return invoiceLine;
    } else {
      if (invoice.getCompany().getAddress().getState() == null) {
        return invoiceLine;
      } else if (invoice.getAddress().getState() == null) {
        return invoiceLine;
      } else {
        BigDecimal calculate = BigDecimal.ZERO;
        calculate = invoiceLine.getPrice().multiply(invoiceLine.getQty());
        invoiceLine.setNetAmmount(calculate);
        if (invoice
            .getCompany()
            .getAddress()
            .getState()
            .getName()
            .equals(invoice.getAddress().getState().getName())) {
          calculate =
              invoiceLine
                  .getNetAmmount()
                  .multiply(invoiceLine.getGstRate().divide(BigDecimal.valueOf(2)));
          invoiceLine.setCgst(calculate);
          invoiceLine.setSgst(calculate);
        } else {
          calculate = (invoiceLine.getNetAmmount().multiply(invoiceLine.getGstRate()));
          invoiceLine.setIgst(calculate);
        }
        calculate = (invoiceLine.getNetAmmount().add(calculate));
        invoiceLine.setGrossAmount(calculate);
      }
    }
    return invoiceLine;
  }
}
