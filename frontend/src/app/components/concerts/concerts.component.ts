import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Concert } from '../../models/community.model';
import { ConcertService } from '../../services/concert.service';

@Component({
  selector: 'app-concerts',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './concerts.component.html',
  styleUrl: './concerts.component.css'
})
export class ConcertsComponent {
  form = this.formBuilder.group({ artist: ['', Validators.required], city: [''] });
  concerts: Concert[] = [];
  loading = false;
  errorMessage = '';
  searched = false;

  constructor(
    private formBuilder: FormBuilder,
    private concertService: ConcertService
  ) {}

  search(): void {
    if (this.form.invalid) return;

    this.loading = true;
    this.errorMessage = '';
    this.concertService.search(
      this.form.value.artist || '',
      this.form.value.city || ''
    ).subscribe({
      next: concerts => {
        this.concerts = concerts;
        this.loading = false;
        this.searched = true;
      },
      error: error => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Could not search concerts';
      }
    });
  }
}
