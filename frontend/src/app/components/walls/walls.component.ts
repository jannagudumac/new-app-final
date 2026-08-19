import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { MusicWall } from '../../models/music-wall.model';
import { MusicWallService } from '../../services/music-wall.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-walls',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './walls.component.html',
  styleUrl: './walls.component.css'
})
export class WallsComponent implements OnInit {

  walls: MusicWall[] = [];
  wallForm: FormGroup;
  loading = false;
  saving = false;
  showCreateForm = false;
  editingWallId: number | null = null;
  editingWallField: 'name' | 'description' | null = null;
  editingWallValue = '';
  errorMessage = '';

  constructor(
    private musicWallService: MusicWallService,
    private formBuilder: FormBuilder,
    private route: ActivatedRoute,
    public authService: AuthService
  ) {
    this.wallForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', Validators.maxLength(500)],
      wallpaper: ['NONE', Validators.required],
      wallColor: ['#FFFFFF', Validators.required]
    });
  }

  ngOnInit(): void {
    this.showCreateForm = this.route.snapshot.queryParamMap.get('create') === 'true';
    this.loadWalls();
  }

  toggleCreateForm(): void {
    this.showCreateForm = true;
    this.errorMessage = '';
  }

  cancelCreate(): void {
    this.showCreateForm = false;
    this.wallForm.reset({
      name: '',
      description: '',
      wallpaper: 'NONE',
      wallColor: '#FFFFFF'
    });
  }

  loadWalls(): void {
    this.loading = true;
    this.errorMessage = '';

    this.musicWallService.getMyWalls().subscribe({
      next: walls => {
        this.walls = walls;
        this.loading = false;
      },
      error: error => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Could not load walls';
      }
    });
  }

  createWall(): void {
    if (this.wallForm.invalid) {
      this.wallForm.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.errorMessage = '';

    this.musicWallService.createWall(this.wallForm.value).subscribe({
      next: wall => {
        this.walls.unshift(wall);
        this.wallForm.reset();
        this.wallForm.patchValue({
          wallpaper: 'NONE',
          wallColor: '#FFFFFF'
        });
        this.showCreateForm = false;
        this.saving = false;
      },
      error: error => {
        this.saving = false;
        this.errorMessage = error.error?.message || 'Could not create wall';
      }
    });
  }

  startEdit(wall: MusicWall, field: 'name' | 'description'): void {
    if (wall.ownerUsername !== this.authService.getUsername()) return;
    this.editingWallId = wall.id;
    this.editingWallField = field;
    this.editingWallValue = field === 'name' ? wall.name : (wall.description || '');
    setTimeout(() => document.getElementById('wall-card-' + field + '-' + wall.id)?.focus());
  }

  cancelEdit(): void {
    this.editingWallId = null;
    this.editingWallField = null;
    this.editingWallValue = '';
  }

  updateWall(wall: MusicWall): void {
    if (this.editingWallId !== wall.id || !this.editingWallField) return;
    const value = this.editingWallValue.trim();
    if (this.editingWallField === 'name' && !value) return;

    this.musicWallService.updateWall(wall.id, {
      name: this.editingWallField === 'name' ? value : wall.name,
      description: this.editingWallField === 'description' ? value : (wall.description || ''),
      wallpaper: wall.wallpaper || 'NONE',
      wallColor: wall.wallColor || '#FFFFFF'
    }).subscribe({
      next: updatedWall => {
        const index = this.walls.findIndex(item => item.id === wall.id);
        if (index !== -1) {
          this.walls[index] = updatedWall;
        }
        this.cancelEdit();
      },
      error: error => {
        this.errorMessage = error.error?.message || 'Could not update wall';
      }
    });
  }

  deleteWall(wall: MusicWall): void {
    if (wall.ownerUsername !== this.authService.getUsername()) return;
    const confirmed = window.confirm('Delete "' + wall.name + '" and all its contents?');
    if (!confirmed) {
      return;
    }

    this.musicWallService.deleteWall(wall.id).subscribe({
      next: () => {
        this.walls = this.walls.filter(currentWall => currentWall.id !== wall.id);
      },
      error: error => {
        this.errorMessage = error.error?.message || 'Could not delete wall';
      }
    });
  }
}
