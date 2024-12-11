package org.eclipse.edc.mvd.model;

import java.util.List;

public class Building {
  private final String id;
  private String firstName;
  private String lastName;
  private final String address;
  private final float livingSpace;
  private final int meterNumber;
  private final List<Float> warmthTotal;
  private final List<Float> warmWaterTotal;
  private final boolean heatedBasement;
  private final int apartments;
  private final String certificateEndpoint;
  private final String dataTrustee;

  public Building(
      String id,
      String firstName,
      String lastName,
      String address,
      float livingSpace,
      int meterNumber,
      List<Float> warmthTotal,
      List<Float> warmWaterTotal,
      boolean heatedBasement,
      int apartments,
      String certificateEndpoint,
      String dataTrustee) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
    this.address = address;
    this.livingSpace = livingSpace;
    this.meterNumber = meterNumber;
    this.warmthTotal = warmthTotal;
    this.warmWaterTotal = warmWaterTotal;
    this.heatedBasement = heatedBasement;
    this.apartments = apartments;
    this.certificateEndpoint = certificateEndpoint;
    this.dataTrustee = dataTrustee;
  }

  public String getId() {
    return id;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getAddress() {
    return address;
  }

  public int getMeterNumber() {
    return meterNumber;
  }

  public float getLivingSpace() {
    return livingSpace;
  }

  public List<Float> getWarmthTotal() {
    return warmthTotal;
  }

  public List<Float> getWarmWaterTotal() {
    return warmWaterTotal;
  }

  public boolean isHeatedBasement() {
    return heatedBasement;
  }

  public int getApartments() {
    return apartments;
  }

  public String getCertificateEndpoint() {
    return certificateEndpoint;
  }

  public String getDataTrustee() {
    return dataTrustee;
  }
}