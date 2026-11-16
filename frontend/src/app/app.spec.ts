import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Appointment, Availability, Barber, ServiceOffering } from './api.models';
import { ApiService } from './api.service';
import { App } from './app';

const barber: Barber = { id: 'barber-1', name: 'João Silva', active: true };
const service: ServiceOffering = {
  id: 'service-1', name: 'Corte', durationMinutes: 30, price: 45, active: true,
};
const availability: Availability = {
  barberId: barber.id,
  serviceId: service.id,
  date: '2030-01-07',
  durationMinutes: service.durationMinutes,
  availableSlots: ['10:00:00', '10:15:00'],
};
const scheduledAppointment: Appointment = {
  id: 'appointment-1',
  barberId: barber.id,
  serviceId: service.id,
  customerName: 'Ana Souza',
  customerPhone: '11999999999',
  startsAt: '2030-01-07T10:00:00',
  endsAt: '2030-01-07T10:30:00',
  status: 'SCHEDULED',
  createdAt: '2029-12-01T12:00:00',
};
const cancelledAppointment: Appointment = { ...scheduledAppointment, status: 'CANCELLED' };

function createApiMock() {
  return {
    health: vi.fn(() => of({ status: 'UP' })),
    barbers: vi.fn(() => of([barber])),
    createBarber: vi.fn(() => of(barber)),
    services: vi.fn(() => of([service])),
    createService: vi.fn(() => of(service)),
    addWorkingHours: vi.fn(() => of({})),
    availability: vi.fn(() => of(availability)),
    createAppointment: vi.fn(() => of(scheduledAppointment)),
    cancelAppointment: vi.fn(() => of(cancelledAppointment)),
  };
}

describe('App', () => {
  let api: ReturnType<typeof createApiMock>;

  beforeEach(async () => {
    api = createApiMock();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [{ provide: ApiService, useValue: api }],
    }).compileComponents();
  });

  function createFixture() {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    return fixture;
  }

  it('renders the dashboard greeting', () => {
    const fixture = createFixture();
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Bom dia, barbeiro.');
  });

  it('shows the customer form after selecting an available time', async () => {
    const fixture = createFixture();
    const app = fixture.componentInstance;

    app.selectedDate = availability.date;
    app.loadAvailability();
    app.selectSlot('10:00:00');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.booking-form')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.booking-form-heading h4')?.textContent).toContain('10:00');
  });

  it('submits a trimmed booking and shows its confirmation', async () => {
    const fixture = createFixture();
    const app = fixture.componentInstance;
    app.selectedDate = availability.date;
    app.selectSlot('10:00:00');
    app.customerName = '  Ana Souza  ';
    app.customerPhone = ' 11999999999 ';

    app.bookSelectedSlot();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(api.createAppointment).toHaveBeenCalledWith({
      barberId: barber.id,
      serviceId: service.id,
      customerName: 'Ana Souza',
      customerPhone: '11999999999',
      startsAt: '2030-01-07T10:00:00',
    });
    expect(app.appointment).toEqual(scheduledAppointment);
    expect(app.selectedSlot).toBe('');
    expect(app.customerName).toBe('');
    expect(app.customerPhone).toBe('');
    expect(fixture.nativeElement.querySelector('.appointment-summary')?.textContent).toContain('Agendamento confirmado');
  });

  it('disables booking submission while the API request is pending', async () => {
    const fixture = createFixture();
    const app = fixture.componentInstance;
    const response = new Subject<Appointment>();
    app.selectedDate = availability.date;
    app.selectSlot('10:00:00');
    app.customerName = 'Ana Souza';
    app.customerPhone = '11999999999';
    api.createAppointment.mockReturnValue(response.asObservable());
    await fixture.whenStable();
    fixture.detectChanges();

    app.bookSelectedSlot();
    await fixture.whenStable();
    fixture.detectChanges();

    const submitButton = fixture.nativeElement.querySelector('.booking-form button[type="submit"]') as HTMLButtonElement;
    expect(app.saving).toBe(true);
    expect(submitButton.disabled).toBe(true);
    expect(submitButton.textContent).toContain('Confirmando...');

    response.next(scheduledAppointment);
    response.complete();
  });

  it('shows a fallback message when booking fails unexpectedly', async () => {
    const fixture = createFixture();
    const app = fixture.componentInstance;
    app.selectedDate = availability.date;
    app.selectSlot('10:00:00');
    app.customerName = 'Ana Souza';
    app.customerPhone = '11999999999';
    api.createAppointment.mockReturnValue(throwError(() => ({ status: 500 })));

    app.bookSelectedSlot();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(app.error).toContain('Não foi possível confirmar o agendamento');
    expect(app.saving).toBe(false);
    expect(app.appointment).toBeNull();
    expect(fixture.nativeElement.querySelector('.notice.danger')?.textContent).toContain('Não foi possível confirmar');
  });

  it('explains a booking conflict and reloads availability', () => {
    const fixture = createFixture();
    const app = fixture.componentInstance;
    app.selectedDate = availability.date;
    app.selectSlot('10:00:00');
    app.customerName = 'Ana Souza';
    app.customerPhone = '11999999999';
    api.createAppointment.mockReturnValue(throwError(() => ({ status: 409 })));
    const availabilityCallsBeforeBooking = api.availability.mock.calls.length;

    app.bookSelectedSlot();
    fixture.detectChanges();

    expect(app.error).toContain('Esse horário acabou de ser ocupado');
    expect(app.saving).toBe(false);
    expect(app.selectedSlot).toBe('');
    expect(api.availability).toHaveBeenCalledTimes(availabilityCallsBeforeBooking + 1);
  });

  it('cancels the current booking and refreshes availability', () => {
    const fixture = createFixture();
    const app = fixture.componentInstance;
    app.selectedDate = availability.date;
    app.appointment = scheduledAppointment;
    const availabilityCallsBeforeCancellation = api.availability.mock.calls.length;

    app.cancelAppointment();
    fixture.detectChanges();

    expect(api.cancelAppointment).toHaveBeenCalledWith(scheduledAppointment.id);
    expect(app.appointment).toEqual(cancelledAppointment);
    expect(app.message).toContain('Agendamento cancelado');
    expect(api.availability).toHaveBeenCalledTimes(availabilityCallsBeforeCancellation + 1);
    expect(fixture.nativeElement.querySelector('.appointment-summary .button')).toBeNull();
  });
});
