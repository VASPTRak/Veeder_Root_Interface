package com.TrakEngineering.veeder_rootinterface.enity;

/**
 * Created by Sven Wijtmans on 4/3/2018.
 * This 'class' contains the information that we want to send to the cloud for the tank monitor for a delivery report
 */

public class VR_Delivery_InfoEntity {

    public String IMEI_UDID;
    public String VeederRootMacAddress;
    public String VRDateTime;
    public String AppInfo;

    public String AppDateTime;
    public String TankNumber;

    public String ProductCode;
    public String StartDateTime;
    public String EndDateTime;
    public String StartVolume;
    public String StartTCVolume;
    public String StartWater;
    public String StartTemp;
    public String EndVolume;
    public String EndTCVolume;
    public String EndWater;
    public String EndTemp;
    public String StartHeight;
    public String EndHeight;
}
