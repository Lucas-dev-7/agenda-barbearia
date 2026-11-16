export interface Barber {
  id: string;
  name: string;
  active: boolean;
}

export interface ServiceOffering {
  id: string;
  name: string;
  durationMinutes: number;
  price: number;
  active: boolean;
}

export interface Availability {
  barberId: string;
  serviceId: string;
  date: string;
  durationMinutes: number;
  availableSlots: string[];
}
