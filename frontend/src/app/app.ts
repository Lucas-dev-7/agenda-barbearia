import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from './api.service';
import { Barber, Availability, ServiceOffering } from './api.models';

@Component({
  imports: [CommonModule, FormsModule],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App implements OnInit {
  barbers: Barber[] = [];
  services: ServiceOffering[] = [];
  availability: Availability | null = null;
  apiOnline = false;
  loadingAvailability = false;
  saving = false;
  message = '';
  error = '';
  selectedBarberId = '';
  selectedServiceId = '';
  selectedDate = this.today();
  readonly minimumDate = this.today();
  barberName = '';
  serviceName = '';
  serviceDuration = 30;
  servicePrice = 0;
  hoursBarberId = '';
  weekday = 1;
  opensAt = '09:00';
  closesAt = '18:00';
  readonly weekdays = [
    { value: 1, label: 'Segunda-feira' },
    { value: 2, label: 'Terça-feira' },
    { value: 3, label: 'Quarta-feira' },
    { value: 4, label: 'Quinta-feira' },
    { value: 5, label: 'Sexta-feira' },
    { value: 6, label: 'Sábado' },
    { value: 7, label: 'Domingo' },
  ];

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.api.health().subscribe({ next: () => (this.apiOnline = true), error: () => (this.apiOnline = false) });
    this.api.barbers().subscribe({
      next: (items) => {
        this.barbers = items;
        if (!this.selectedBarberId && items.length) this.selectedBarberId = items[0].id;
        if (!this.hoursBarberId && items.length) this.hoursBarberId = items[0].id;
        this.loadAvailability();
      },
      error: (e) => this.showError(e, 'Não foi possível carregar os barbeiros.'),
    });
    this.api.services().subscribe({
      next: (items) => {
        this.services = items;
        if (!this.selectedServiceId && items.length) this.selectedServiceId = items[0].id;
        this.loadAvailability();
      },
      error: (e) => this.showError(e, 'Não foi possível carregar os serviços.'),
    });
  }

  createBarber(): void {
    if (!this.barberName.trim()) return;
    this.saving = true;
    this.clearNotice();
    this.api.createBarber(this.barberName.trim()).subscribe({
      next: (created) => {
        this.barberName = '';
        this.selectedBarberId = created.id;
        this.hoursBarberId = created.id;
        this.message = 'Barbeiro cadastrado com sucesso.';
        this.saving = false;
        this.refresh();
      },
      error: (e) => { this.showError(e, 'Não foi possível cadastrar o barbeiro.'); this.saving = false; },
    });
  }

  createService(): void {
    if (!this.serviceName.trim() || this.serviceDuration < 1 || this.servicePrice < 0) return;
    this.saving = true;
    this.clearNotice();
    this.api.createService(this.serviceName.trim(), this.serviceDuration, this.servicePrice).subscribe({
      next: (created) => {
        this.serviceName = '';
        this.selectedServiceId = created.id;
        this.message = 'Serviço cadastrado com sucesso.';
        this.saving = false;
        this.refresh();
      },
      error: (e) => { this.showError(e, 'Não foi possível cadastrar o serviço.'); this.saving = false; },
    });
  }

  saveWorkingHours(): void {
    if (!this.hoursBarberId) return;
    this.saving = true;
    this.clearNotice();
    this.api.addWorkingHours(this.hoursBarberId, this.weekday, `${this.opensAt}:00`, `${this.closesAt}:00`).subscribe({
      next: () => {
        this.message = 'Expediente cadastrado. Consulte a disponibilidade ao lado.';
        this.saving = false;
        this.loadAvailability();
      },
      error: (e) => { this.showError(e, 'Não foi possível cadastrar o expediente.'); this.saving = false; },
    });
  }

  loadAvailability(): void {
    if (!this.selectedBarberId || !this.selectedServiceId || !this.selectedDate) {
      this.availability = null;
      return;
    }
    this.loadingAvailability = true;
    this.api.availability(this.selectedBarberId, this.selectedServiceId, this.selectedDate).subscribe({
      next: (result) => { this.availability = result; this.loadingAvailability = false; },
      error: (e) => {
        this.availability = null;
        this.loadingAvailability = false;
        this.showError(e, 'Não foi possível buscar os horários. Confira o expediente do barbeiro.');
      },
    });
  }

  private today(): string {
    const date = new Date();
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
    return local.toISOString().slice(0, 10);
  }

  private clearNotice(): void { this.message = ''; this.error = ''; }

  private showError(error: { status?: number }, fallback: string): void {
    this.message = '';
    this.error = error.status === 0
      ? 'Sem conexão com a API. Inicie o back-end Spring Boot na porta 8080.'
      : fallback;
  }
}
