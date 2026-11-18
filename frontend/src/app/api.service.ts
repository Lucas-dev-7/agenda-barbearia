import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Appointment, Availability, Barber, CreateAppointmentRequest, ServiceOffering } from './api.models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly apiUrl = '/api';

  constructor(private readonly http: HttpClient) {}

  csrf(): Observable<void> {
    return this.http.get<void>(`${this.apiUrl}/auth/csrf`);
  }

  currentUser(): Observable<{ username: string }> {
    return this.http.get<{ username: string }>(`${this.apiUrl}/auth/me`);
  }

  login(username: string, password: string): Observable<void> {
    const body = new HttpParams().set('username', username).set('password', password);
    return this.http.post<void>(`${this.apiUrl}/auth/login`, body, {
      headers: new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' }),
    });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/auth/logout`, {});
  }

  health(): Observable<{ status: string }> {
    return this.http.get<{ status: string }>('/actuator/health');
  }

  barbers(): Observable<Barber[]> {
    return this.http.get<Barber[]>(`${this.apiUrl}/barbers`);
  }

  createBarber(name: string): Observable<Barber> {
    return this.http.post<Barber>(`${this.apiUrl}/barbers`, { name });
  }

  services(): Observable<ServiceOffering[]> {
    return this.http.get<ServiceOffering[]>(`${this.apiUrl}/services`);
  }

  createService(name: string, durationMinutes: number, price: number): Observable<ServiceOffering> {
    return this.http.post<ServiceOffering>(`${this.apiUrl}/services`, { name, durationMinutes, price });
  }

  addWorkingHours(barberId: string, dayOfWeek: number, opensAt: string, closesAt: string): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/barbers/${barberId}/working-hours`, { dayOfWeek, opensAt, closesAt });
  }

  availability(barberId: string, serviceId: string, date: string): Observable<Availability> {
    const params = new HttpParams().set('date', date).set('serviceId', serviceId);
    return this.http.get<Availability>(`${this.apiUrl}/barbers/${barberId}/availability`, { params });
  }

  createAppointment(request: CreateAppointmentRequest): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.apiUrl}/appointments`, request);
  }

  cancelAppointment(appointmentId: string): Observable<Appointment> {
    return this.http.patch<Appointment>(`${this.apiUrl}/appointments/${appointmentId}/cancel`, {});
  }
}
