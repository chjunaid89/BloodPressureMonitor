#include <LiquidCrystal.h>
LiquidCrystal lcd( 12,13,5,4,3,2);
void(* resetFunc) (void) = 0;


const int motorPin = 6;

int START; // counter to run program once and end

const int transducer = A1;
const int dumpValve = 9;
float PressureMax = 5;
float PressureMinc= -5;
float maxvoltc= 0;
float  p_psi = 0;
float supply = 5;
int x[] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,};    // array for pressure values
int timem[] = {200,200,200,200,200,200,200,200,200,200,200,200,200,200,200};  // delay for deflating the cuff
int y [] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,};  // array for maximum pressure values
int p1;
int p2;
int j;
int speed = 128 ; // 128 = 3.3 volt
int maxm = 0;
int i;
int index[] = {0,0,0,0,0,0,0,0,0,0};
int k;
int l = 0;
int total = 0;
int a;

int sensorValue[3];  // array to save results
char input = 0;
 
void setup() {
  
  Serial.begin(9600);
  lcd.begin(16,2);
  lcd.print ("Junaid");
 
  pinMode(motorPin, OUTPUT);
  pinMode(dumpValve, OUTPUT);
  
}

void loop() {
  
    if (Serial.available() > 0)
  {
    input = Serial.read();
    if (input == '0')
    {
      resetFunc();    // reset the device
    }
    if (input == '1')
    {
      START = 1;
    }
  }
  
if(START == 1){

// delay to open serial monitor

delay(200);

// close the dummp valve

analogWrite(dumpValve, 674);  // 674 = 3.3 volt

// start motor

analogWrite (motorPin, speed);

// read pressure in the cuff

int pressure = analogRead(transducer);

// read untill the pressure value is 180mmHg

while(pressure<=797)      // 797 = 3.89 volt 
{

pressure = analogRead(transducer);

delay(55);

}

// stop motor

speed = 0;

analogWrite(motorPin, speed);

delay(2000);

// dump valve code

// reading pressure values while deflating the cuff

for(j=0;j<15;j=j+1){

analogWrite(dumpValve, 0);

delay(timem[j]);

analogWrite(dumpValve, 674);

delay(250);

pressure = analogRead(transducer);

x[j]=pressure;

p1= analogRead(transducer);

delay(1000);

p2=analogRead(transducer);

delay(200);

y[j]= max(p1,p2);

}

// find maximum fluctuation value

for(i=3; i <15;i=i+1){

if(y[i]>maxm){

maxm=y[i];
} 
}

// find the index of maximum fluctuation value

for(k=3;k<15;k=k+1){

if(y[k]==maxm){

index[k-3] = k;

l++;

}

}

// conversion of the sensor values into psi and mmHg

for(a=0; a<l;a++){
  
maxvolt= ( ((maxm)*supply)/1023);

p_psi = ((((maxvolt)-.1*supply)/((.8*supply)/(PressureMax-PressureMin)))+ PressureMin);

int MAP = p_psi*51.7;

sensorValue[0] = MAP;  // Mean Arterial Pressure

int sys = MAP * 0.55;

sys = MAP + sys;

sensorValue[1] = sys;  // Systolic Pressure

int dia = MAP * 0.18;

dia = MAP - dia;

sensorValue[2] = dia;  // Diastolic Pressure

int pulsePressure = (sys-dia);

lcd.setCursor(0,0);

lcd.print("   ");

lcd.print("MAP=");

lcd.print(sensorValue[0]);

lcd.setCursor(0,1);

lcd.print("sys=");

lcd.print(sensorValue[1]);

lcd.print("/");

lcd.print("dia=");

lcd.print(sensorValue[2]);
}

analogWrite(dumpValve, 0); // open the dump valve

START = 0;

sendAndroidValues(); // transmitt data to Android

}

}


//sends the measured values to Android through Bluetooth module

void sendAndroidValues()
 {
  Serial.print('#'); // start of transmission
  for(int k=0; k<3; k++)
  {
    Serial.print(sensorValue[k]);
    Serial.print('+'); //to break up data values
  }
 Serial.print('~'); //end of transmission
 Serial.println();
 delay(10);        //delay to eliminate missed transmissions
}
