package com.TrakEngineering.veeder_rootinterface.enity;

/**
 * Created by Sven Wijtmans on 1/18/2018.
 * This 'class' contains the information that we want to send to the cloud for the tank monitor for an inventory report
 */

public class VR_Inventory_InfoEntity {

    public String IMEI_UDID;
    public String VeederRootMacAddress;
    public String TankNumber;
    public String AppDateTime;
    public String VRDateTime;
    public String ProductCode;
    public String TankStatus;
    public String Volume;
    public String TCVolume;
    public String Ullage;
    public String Height;
    public String Water;
    public String Temperature;
    public String WaterVolume;
    public String ForceReadingSave;

    public String AppInfo;
}
