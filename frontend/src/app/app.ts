import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from './api.service';
import { Appointment, Barber, Availability, ServiceOffering } from './api.models';

@Component({
  imports: [CommonModule, FormsModule],
  selector: 'app-root',
  styleUrls: ['./app.css', './login.css'],
  templateUrl: './app.html',
})
export class App implements OnInit {
  authenticated = false;
  authLoading = true;
  loginSaving = false;
  loginUsername = '';
  loginPassword = '';
  loginError = '';
  currentUsername = '';
  barbers: Barber[] = [];
  services: ServiceOffering[] = [];
  availability: Availability | null = null;
  appointment: Appointment | null = null;
  apiOnline = false;
  loadingAvailability = false;
  saving = false;
  message = '';
  error = '';
  selectedBarberId = '';
  selectedServiceId = '';
  selectedDate = this.today();
  selectedSlot = '';
  customerName = '';
  customerPhone = '';
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

  constructor(
    private readonly api: ApiService,
    private readonly changeDetector: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.initializeSession();
  }

  initializeSession(): void {
    this.api.csrf().subscribe({
      next: () => this.api.currentUser().subscribe({
        next: (user) => {
          this.authenticated = true;
          this.currentUsername = user.username;
          this.authLoading = false;
          this.refresh();
          this.changeDetector.markForCheck();
        },
        error: (error) => {
          this.authLoading = false;
          if (error.status !== 401) this.loginError = 'Não foi possível verificar a sessão.';
          this.changeDetector.markForCheck();
        },
      }),
      error: () => {
        this.authLoading = false;
        this.loginError = 'Não foi possível conectar à API. Inicie o back-end Spring Boot na porta 8080.';
        this.changeDetector.markForCheck();
      },
    });
  }

  login(): void {
    if (!this.loginUsername.trim() || !this.loginPassword) return;
    this.loginSaving = true;
    this.loginError = '';
    this.api.login(this.loginUsername.trim(), this.loginPassword).subscribe({
      next: () => this.api.csrf().subscribe({
        next: () => this.api.currentUser().subscribe({
          next: (user) => {
            this.authenticated = true;
            this.currentUsername = user.username;
            this.loginPassword = '';
            this.loginSaving = false;
            this.refresh();
            this.changeDetector.markForCheck();
          },
          error: () => {
            this.loginSaving = false;
            this.loginError = 'Não foi possível validar a sessão. Tente entrar novamente.';
            this.changeDetector.markForCheck();
          },
        }),
        error: () => {
          this.loginSaving = false;
          this.loginError = 'Não foi possível iniciar uma sessão segura. Tente novamente.';
          this.changeDetector.markForCheck();
        },
      }),
      error: (error) => {
        this.loginSaving = false;
        this.loginError = error.status === 401
          ? 'Usuário ou senha incorretos.'
          : 'Não foi possível entrar. Verifique a conexão e tente novamente.';
        this.changeDetector.markForCheck();
      },
    });
  }

  logout(): void {
    this.api.logout().subscribe({
      next: () => this.api.csrf().subscribe({
        next: () => {
          this.authenticated = false;
          this.currentUsername = '';
          this.loginPassword = '';
          this.clearNotice();
          this.changeDetector.markForCheck();
        },
        error: () => {
          this.authenticated = false;
          this.currentUsername = '';
          this.loginError = 'Sessão encerrada. Atualize a página para entrar novamente.';
          this.changeDetector.markForCheck();
        },
      }),
      error: (error) => {
        if (error.status === 401) {
          this.authenticated = false;
          this.currentUsername = '';
        }
        this.loginError = 'Não foi possível encerrar a sessão. Atualize a página e tente novamente.';
        this.changeDetector.markForCheck();
      },
    });
  }

  refresh(): void {
    this.api.health().subscribe({
      next: (health) => {
        this.apiOnline = health.status === 'UP';
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.apiOnline = false;
        this.changeDetector.markForCheck();
      },
    });
    this.api.barbers().subscribe({
      next: (items) => {
        this.apiOnline = true;
        this.barbers = items;
        if (!this.selectedBarberId && items.length) this.selectedBarberId = items[0].id;
        if (!this.hoursBarberId && items.length) this.hoursBarberId = items[0].id;
        this.loadAvailability();
        this.changeDetector.markForCheck();
      },
      error: (e) => {
        if (e.status === 0) this.apiOnline = false;
        this.showError(e, 'Não foi possível carregar os barbeiros.');
        this.changeDetector.markForCheck();
      },
    });
    this.api.services().subscribe({
      next: (items) => {
        this.apiOnline = true;
        this.services = items;
        if (!this.selectedServiceId && items.length) this.selectedServiceId = items[0].id;
        this.loadAvailability();
        this.changeDetector.markForCheck();
      },
      error: (e) => {
        if (e.status === 0) this.apiOnline = false;
        this.showError(e, 'Não foi possível carregar os serviços.');
        this.changeDetector.markForCheck();
      },
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
        this.changeDetector.markForCheck();
      },
      error: (e) => {
        this.showError(e, 'Não foi possível cadastrar o barbeiro.');
        this.saving = false;
        this.changeDetector.markForCheck();
      },
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
        this.changeDetector.markForCheck();
      },
      error: (e) => {
        this.showError(e, 'Não foi possível cadastrar o serviço.');
        this.saving = false;
        this.changeDetector.markForCheck();
      },
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
        this.changeDetector.markForCheck();
      },
      error: (e) => {
        this.showError(e, 'Não foi possível cadastrar o expediente.');
        this.saving = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  loadAvailability(): void {
    this.selectedSlot = '';
    if (!this.selectedBarberId || !this.selectedServiceId || !this.selectedDate) {
      this.availability = null;
      return;
    }
    this.loadingAvailability = true;
    this.api.availability(this.selectedBarberId, this.selectedServiceId, this.selectedDate).subscribe({
      next: (result) => {
        this.availability = result;
        this.loadingAvailability = false;
        this.changeDetector.markForCheck();
      },
      error: (e) => {
        this.availability = null;
        this.loadingAvailability = false;
        this.showError(e, 'Não foi possível buscar os horários. Confira o expediente do barbeiro.');
        this.changeDetector.markForCheck();
      },
    });
  }

  selectSlot(slot: string): void {
    this.selectedSlot = this.selectedSlot === slot ? '' : slot;
    this.clearNotice();
  }

  bookSelectedSlot(): void {
    if (!this.selectedBarberId || !this.selectedServiceId || !this.selectedSlot) return;
    const customerName = this.customerName.trim();
    const customerPhone = this.customerPhone.trim();
    if (!customerName || !customerPhone) return;

    this.saving = true;
    this.clearNotice();
    this.api.createAppointment({
      barberId: this.selectedBarberId,
      serviceId: this.selectedServiceId,
      customerName,
      customerPhone,
      startsAt: `${this.selectedDate}T${this.selectedSlot.slice(0, 5)}:00`,
    }).subscribe({
      next: (created) => {
        this.appointment = created;
        this.selectedSlot = '';
        this.customerName = '';
        this.customerPhone = '';
        this.message = 'Agendamento confirmado. Guarde o resumo abaixo.';
        this.saving = false;
        this.loadAvailability();
        this.changeDetector.markForCheck();
      },
      error: (e) => {
        this.showError(e, e.status === 409
          ? 'Esse horário acabou de ser ocupado. Escolha outro horário disponível.'
          : 'Não foi possível confirmar o agendamento. Confira os dados e tente novamente.');
        this.saving = false;
        if (e.status === 409) this.loadAvailability();
        this.changeDetector.markForCheck();
      },
    });
  }

  cancelAppointment(): void {
    if (!this.appointment || this.appointment.status !== 'SCHEDULED') return;
    this.saving = true;
    this.clearNotice();
    this.api.cancelAppointment(this.appointment.id).subscribe({
      next: (cancelled) => {
        this.appointment = cancelled;
        this.message = 'Agendamento cancelado e horário liberado.';
        this.saving = false;
        this.loadAvailability();
        this.changeDetector.markForCheck();
      },
      error: (e) => {
        this.showError(e, e.status === 409
          ? 'Este agendamento não pode mais ser cancelado.'
          : 'Não foi possível cancelar o agendamento. Tente novamente.');
        this.saving = false;
        this.changeDetector.markForCheck();
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
    if (error.status === 401) {
      this.authenticated = false;
      this.loginError = 'Sua sessão expirou. Entre novamente.';
    }
    this.message = '';
    this.error = error.status === 0
      ? 'Sem conexão com a API. Inicie o back-end Spring Boot na porta 8080.'
      : fallback;
  }
}
